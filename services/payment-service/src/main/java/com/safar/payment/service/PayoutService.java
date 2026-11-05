package com.safar.payment.service;

import com.safar.payment.entity.Payout;
import com.safar.payment.entity.PayoutItem;
import com.safar.payment.entity.SettlementLine;
import com.safar.payment.entity.enums.PayoutStatus;
import com.safar.payment.entity.enums.RecipientType;
import com.safar.payment.entity.enums.SettlementStatus;
import com.safar.payment.repository.PayoutItemRepository;
import com.safar.payment.repository.PayoutRepository;
import com.safar.payment.repository.SettlementLineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Pays settled HOST lines out via RazorpayX.
 * <ul>
 *   <li><b>Eligibility</b>: a line is payable only at/after {@code payout_eligible_at}
 *       (checkout + dispute window). Admin can {@code force} past it.</li>
 *   <li><b>Batching</b>: all eligible, unpaid lines of one host go out as ONE payout, tracked in
 *       {@code payout_items}. A line can belong to at most one payout (unique index).</li>
 * </ul>
 * See docs/design-host-payout.md.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PayoutService {

    private final PayoutRepository payoutRepo;
    private final PayoutItemRepository itemRepo;
    private final SettlementLineRepository lineRepo;
    private final RazorpayXClient razorpayX;
    private final RestTemplate restTemplate;
    private final PayoutEligibility eligibility;

    @Value("${services.user-service.url}")
    private String userServiceUrl;
    @Value("${payouts.max-retries:3}")
    private int maxRetries;

    // ── Eligibility ──────────────────────────────────────────────────────────

    /** Lazily fills a missing eligibility date (e.g. lines settled before V11). Unknown = stays null. */
    private boolean isEligible(SettlementLine line, OffsetDateTime now) {
        if (line.getPayoutEligibleAt() == null) {
            eligibility.eligibleAt(line.getPlan().getBookingId()).ifPresent(at -> {
                line.setPayoutEligibleAt(at);
                lineRepo.save(line);
            });
        }
        return line.getPayoutEligibleAt() != null && !line.getPayoutEligibleAt().isAfter(now);
    }

    /** Unpaid settled HOST lines grouped by host, each tagged with whether it is payable now. */
    @Transactional
    public Map<UUID, List<SettlementLine>> eligibleLinesByHost() {
        OffsetDateTime now = OffsetDateTime.now();
        Map<UUID, List<SettlementLine>> out = new LinkedHashMap<>();
        for (SettlementLine l : lineRepo.findUnpaidHostLines()) {
            if (isEligible(l, now)) out.computeIfAbsent(l.getRecipientId(), k -> new ArrayList<>()).add(l);
        }
        return out;
    }

    // ── Execute ──────────────────────────────────────────────────────────────

    /** Pays one settlement line on its own. Idempotent. {@code force} skips the eligibility date. */
    @Transactional
    public Payout executeForSettlementLine(UUID lineId, boolean force) {
        SettlementLine line = lineRepo.findById(lineId)
                .orElseThrow(() -> new IllegalArgumentException("Settlement line not found: " + lineId));
        validateLine(line);

        Optional<Payout> existing = payoutRepo.findBySettlementLineId(lineId);
        if (existing.isPresent()) return existing.get().getStatus() == PayoutStatus.PENDING ? send(existing.get()) : existing.get();

        if (!itemRepo.findBySettlementLineIdIn(List.of(lineId)).isEmpty()) {
            throw new IllegalStateException("Line already included in another payout");
        }
        if (!force && !isEligible(line, OffsetDateTime.now())) {
            throw new IllegalStateException("Not payable yet (eligible at " + line.getPayoutEligibleAt()
                    + "). Use force to override.");
        }
        Payout payout = payoutRepo.save(newPayout(line.getRecipientId(), List.of(line), lineId));
        itemRepo.save(item(payout, line));
        return send(payout);
    }

    /** Pays all eligible unpaid lines of a host as one payout. Empty if nothing is payable. */
    @Transactional
    public Optional<Payout> executeBatchForHost(UUID hostId, boolean force) {
        OffsetDateTime now = OffsetDateTime.now();
        List<SettlementLine> lines = new ArrayList<>();
        for (SettlementLine l : lineRepo.findUnpaidHostLines()) {
            if (!hostId.equals(l.getRecipientId())) continue;
            if (force || isEligible(l, now)) lines.add(l);
        }
        if (lines.isEmpty()) return Optional.empty();
        lines.forEach(this::validateLine);

        Payout payout = payoutRepo.save(newPayout(hostId, lines, lines.size() == 1 ? lines.get(0).getId() : null));
        lines.forEach(l -> itemRepo.save(item(payout, l)));
        log.info("Batched payout {} for host {}: {} lines, {} paise", payout.getId(), hostId, lines.size(),
                payout.getNetAmountPaise());
        return Optional.of(send(payout));
    }

    /** Scheduler entry: one batched payout per host with eligible lines. Never throws. */
    public int executeAllEligible() {
        int sent = 0;
        for (UUID hostId : eligibleLinesByHost().keySet()) {
            try {
                if (executeBatchForHost(hostId, false).isPresent()) sent++;
            } catch (Exception e) {
                log.error("Batch payout for host {} failed: {}", hostId, e.getMessage());
            }
        }
        return sent;
    }

    @Transactional
    public Payout retry(UUID payoutId) {
        Payout payout = payoutRepo.findById(payoutId)
                .orElseThrow(() -> new IllegalArgumentException("Payout not found: " + payoutId));
        if (payout.getStatus() != PayoutStatus.FAILED && payout.getStatus() != PayoutStatus.REVERSED) {
            throw new IllegalStateException("Can only retry FAILED or REVERSED payouts. Current: " + payout.getStatus());
        }
        if (payout.getRetryCount() != null && payout.getRetryCount() >= maxRetries) {
            throw new IllegalStateException("Retry limit (" + maxRetries + ") reached — investigate before retrying");
        }
        // Fresh idempotency key: the previous attempt ended in a terminal failure.
        payout.setIdempotencyKey(null);
        return send(payout);
    }

    // ── Internals ────────────────────────────────────────────────────────────

    private void validateLine(SettlementLine line) {
        if (line.getRecipientType() != RecipientType.HOST) {
            throw new IllegalArgumentException("Only HOST settlement lines can be paid out");
        }
        if (line.getStatus() != SettlementStatus.COMPLETED) {
            throw new IllegalStateException("Settle the booking first (line status " + line.getStatus() + ")");
        }
        if (line.getAmountPaise() == null || line.getAmountPaise() <= 0) {
            throw new IllegalStateException("Nothing to pay out (amount " + line.getAmountPaise() + ")");
        }
        if (line.getRecipientId() == null) {
            throw new IllegalStateException("Settlement line has no host");
        }
    }

    private Payout newPayout(UUID hostId, List<SettlementLine> lines, UUID singleLineId) {
        long total = lines.stream().mapToLong(SettlementLine::getAmountPaise).sum();
        return Payout.builder()
                .hostId(hostId)
                .bookingId(lines.size() == 1 ? lines.get(0).getPlan().getBookingId() : null)
                .amountPaise(total)
                .netAmountPaise(total)
                .method("BANK")
                .settlementLineId(singleLineId)
                .status(PayoutStatus.PENDING)
                .build();
    }

    private PayoutItem item(Payout payout, SettlementLine line) {
        return PayoutItem.builder()
                .payoutId(payout.getId())
                .settlementLineId(line.getId())
                .bookingId(line.getPlan().getBookingId())
                .amountPaise(line.getAmountPaise())
                .build();
    }

    @SuppressWarnings("unchecked")
    private Payout send(Payout payout) {
        try {
            Map<String, Object> d = restTemplate.getForObject(
                    userServiceUrl + "/api/v1/internal/hosts/{id}/payout-details", Map.class, payout.getHostId());
            if (d == null || !Boolean.TRUE.equals(d.get("kycVerified"))) {
                throw new IllegalStateException("Host KYC/bank not verified: " + (d == null ? "no response" : d.get("reason")));
            }
            String ifsc = String.valueOf(d.get("ifsc"));
            String acct = String.valueOf(d.get("accountNumber"));
            String name = String.valueOf(d.get("name"));
            if (ifsc.isBlank() || acct.isBlank() || "null".equals(acct) || "null".equals(ifsc)) {
                throw new IllegalStateException("Host bank details incomplete");
            }

            // Stable across HTTP retries of this attempt; regenerated by retry() after a terminal failure.
            if (payout.getIdempotencyKey() == null) {
                payout.setIdempotencyKey(UUID.randomUUID().toString().replace("-", ""));
            }
            payoutRepo.saveAndFlush(payout); // persist the key before the money call

            String contactId = razorpayX.createContact(name, String.valueOf(d.getOrDefault("email", "")),
                    "host-" + payout.getHostId().toString().replace("-", ""));
            String fundAccountId = razorpayX.createBankFundAccount(contactId, name, ifsc, acct);
            var res = razorpayX.createPayout(fundAccountId, false, payout.getNetAmountPaise(), "payout",
                    payout.getId().toString().replace("-", ""), payout.getIdempotencyKey());

            payout.setFundAccountId(fundAccountId);
            payout.setRazorpayPayoutId(res.payoutId());
            payout.setStatus(PayoutStatus.PROCESSING);
            payout.setFailureReason(null);
            payout.setInitiatedAt(OffsetDateTime.now());
            log.info("Payout {} sent to RazorpayX: {} ({} paise, host {})", payout.getId(), res.payoutId(),
                    payout.getNetAmountPaise(), payout.getHostId());
        } catch (Exception e) {
            payout.setStatus(PayoutStatus.FAILED);
            payout.setFailureReason(trunc(e.getMessage()));
            payout.setRetryCount((payout.getRetryCount() == null ? 0 : payout.getRetryCount()) + 1);
            log.error("Payout {} failed: {}", payout.getId(), e.getMessage());
        }
        return payoutRepo.save(payout);
    }

    private static String trunc(String s) {
        if (s == null) return "unknown error";
        return s.length() <= 480 ? s : s.substring(0, 480);
    }
}
