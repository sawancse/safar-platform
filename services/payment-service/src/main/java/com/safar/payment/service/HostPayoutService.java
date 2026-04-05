package com.safar.payment.service;

import com.safar.payment.entity.HostPayout;
import com.safar.payment.entity.enums.PayoutStatus;
import com.safar.payment.repository.HostPayoutRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class HostPayoutService {

    private final HostPayoutRepository payoutRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RestTemplate restTemplate;
    private final String userServiceUrl;

    // TDS threshold: 1% u/s 194-IB for monthly rent > ₹50,000
    private static final long TDS_THRESHOLD_PAISE = 5000000L; // ₹50,000
    private static final int TDS_RATE_BPS = 100; // 1%
    private static final int GST_RATE_BPS = 1800; // 18%

    public HostPayoutService(HostPayoutRepository payoutRepository,
                             KafkaTemplate<String, Object> kafkaTemplate,
                             RestTemplate restTemplate,
                             @Value("${services.user-service.url}") String userServiceUrl) {
        this.payoutRepository = payoutRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.restTemplate = restTemplate;
        this.userServiceUrl = userServiceUrl;
    }

    @Transactional
    public HostPayout processRentPayout(UUID tenancyId, UUID invoiceId,
                                         long grossAmountPaise, UUID hostId,
                                         int commissionBps) {
        // Idempotency: skip if payout already exists for this invoice
        if (payoutRepository.existsByInvoiceId(invoiceId)) {
            log.info("Payout already exists for invoice {}, skipping", invoiceId);
            return null;
        }

        // Calculate commission: Safar's cut
        long commissionPaise = grossAmountPaise * commissionBps / 10000;

        // GST on commission (18%)
        long gstOnCommission = commissionPaise * GST_RATE_BPS / 10000;

        // TDS: 1% if monthly rent exceeds ₹50,000
        long tdsPaise = 0;
        if (grossAmountPaise > TDS_THRESHOLD_PAISE) {
            tdsPaise = grossAmountPaise * TDS_RATE_BPS / 10000;
        }

        // Net payout = gross - commission - GST on commission - TDS
        long netPayout = grossAmountPaise - commissionPaise - gstOnCommission - tdsPaise;

        HostPayout payout = HostPayout.builder()
                .tenancyId(tenancyId)
                .hostId(hostId)
                .invoiceId(invoiceId)
                .grossAmountPaise(grossAmountPaise)
                .commissionRateBps(commissionBps)
                .commissionPaise(commissionPaise)
                .gstOnCommissionPaise(gstOnCommission)
                .tdsAmountPaise(tdsPaise)
                .netPayoutPaise(netPayout)
                .payoutStatus(PayoutStatus.PENDING)
                .settlementPeriodStart(LocalDate.now().withDayOfMonth(1))
                .settlementPeriodEnd(LocalDate.now())
                .build();

        HostPayout saved = payoutRepository.save(payout);
        log.info("Host payout created: {} — gross={}, commission={} ({}bps), GST={}, TDS={}, net={}",
                saved.getId(), grossAmountPaise, commissionPaise, commissionBps,
                gstOnCommission, tdsPaise, netPayout);

        return saved;
    }

    @Transactional
    public HostPayout executeTransfer(UUID payoutId) {
        HostPayout payout = payoutRepository.findById(payoutId)
                .orElseThrow(() -> new RuntimeException("Payout not found: " + payoutId));

        if (payout.getPayoutStatus() != PayoutStatus.PENDING) {
            log.info("Payout {} not PENDING, status: {}", payoutId, payout.getPayoutStatus());
            return payout;
        }

        payout.setPayoutStatus(PayoutStatus.PROCESSING);
        payoutRepository.save(payout);

        try {
            // Fetch host's Razorpay linked account from user-service
            String linkedAccountId = fetchHostLinkedAccount(payout.getHostId());

            if (linkedAccountId == null || linkedAccountId.isEmpty()) {
                // No linked account — payout remains PROCESSING, will be picked up by scheduler
                log.warn("No Razorpay linked account for host {}, payout {} stays PROCESSING",
                        payout.getHostId(), payoutId);
                return payout;
            }

            // In production: Use Razorpay Route API to transfer
            // POST /transfers { account: linkedAccountId, amount: netPayoutPaise, currency: "INR" }
            // For now, simulate successful transfer
            String transferId = "txfr_" + UUID.randomUUID().toString().substring(0, 14);

            payout.setRazorpayTransferId(transferId);
            payout.setPayoutStatus(PayoutStatus.COMPLETED);
            payout.setPayoutDate(LocalDate.now());
            HostPayout saved = payoutRepository.save(payout);

            kafkaTemplate.send("host.payout.completed", payout.getHostId().toString(), Map.of(
                    "payoutId", saved.getId().toString(),
                    "hostId", saved.getHostId().toString(),
                    "netPayoutPaise", saved.getNetPayoutPaise(),
                    "transferId", transferId
            ));

            log.info("Host payout {} completed: net={}, transferId={}",
                    saved.getId(), saved.getNetPayoutPaise(), transferId);
            return saved;

        } catch (Exception e) {
            payout.setPayoutStatus(PayoutStatus.FAILED);
            payout.setFailureReason(e.getMessage());
            payout.setRetryCount(payout.getRetryCount() + 1);
            payoutRepository.save(payout);

            kafkaTemplate.send("host.payout.failed", payout.getHostId().toString(), Map.of(
                    "payoutId", payout.getId().toString(),
                    "hostId", payout.getHostId().toString(),
                    "reason", e.getMessage()
            ));

            log.error("Host payout {} failed: {}", payout.getId(), e.getMessage());
            return payout;
        }
    }

    @Transactional
    public HostPayout retryPayout(UUID payoutId) {
        HostPayout payout = payoutRepository.findById(payoutId)
                .orElseThrow(() -> new RuntimeException("Payout not found: " + payoutId));

        if (payout.getPayoutStatus() != PayoutStatus.FAILED) {
            throw new RuntimeException("Can only retry FAILED payouts, current: " + payout.getPayoutStatus());
        }

        payout.setPayoutStatus(PayoutStatus.PENDING);
        payout.setFailureReason(null);
        payoutRepository.save(payout);

        return executeTransfer(payoutId);
    }

    public Page<HostPayout> getPayouts(UUID hostId, Pageable pageable) {
        return payoutRepository.findByHostIdOrderByCreatedAtDesc(hostId, pageable);
    }

    public Map<String, Object> getPayoutSummary(UUID hostId, int month, int year) {
        List<HostPayout> payouts = payoutRepository.findByHostIdAndMonth(hostId, month, year);

        long totalGross = payouts.stream().mapToLong(HostPayout::getGrossAmountPaise).sum();
        long totalCommission = payouts.stream().mapToLong(HostPayout::getCommissionPaise).sum();
        long totalGst = payouts.stream().mapToLong(HostPayout::getGstOnCommissionPaise).sum();
        long totalTds = payouts.stream().mapToLong(HostPayout::getTdsAmountPaise).sum();
        long totalNet = payouts.stream().mapToLong(HostPayout::getNetPayoutPaise).sum();
        long completed = payouts.stream().filter(p -> p.getPayoutStatus() == PayoutStatus.COMPLETED).count();
        long pending = payouts.stream().filter(p -> p.getPayoutStatus() == PayoutStatus.PENDING ||
                p.getPayoutStatus() == PayoutStatus.PROCESSING).count();

        Map<String, Object> summary = new HashMap<>();
        summary.put("hostId", hostId.toString());
        summary.put("month", month);
        summary.put("year", year);
        summary.put("totalPayouts", payouts.size());
        summary.put("completedPayouts", completed);
        summary.put("pendingPayouts", pending);
        summary.put("totalGrossPaise", totalGross);
        summary.put("totalCommissionPaise", totalCommission);
        summary.put("totalGstPaise", totalGst);
        summary.put("totalTdsPaise", totalTds);
        summary.put("totalNetPayoutPaise", totalNet);
        return summary;
    }

    public List<HostPayout> getPendingPayouts() {
        return payoutRepository.findByPayoutStatus(PayoutStatus.PENDING);
    }

    public List<HostPayout> getFailedPayouts() {
        return payoutRepository.findByPayoutStatus(PayoutStatus.FAILED);
    }

    private String fetchHostLinkedAccount(UUID hostId) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> kyc = restTemplate.getForObject(
                    userServiceUrl + "/api/v1/kyc/host/" + hostId, Map.class);
            if (kyc != null && kyc.get("razorpayLinkedAccountId") != null) {
                return (String) kyc.get("razorpayLinkedAccountId");
            }
        } catch (Exception e) {
            log.warn("Could not fetch KYC for host {}: {}", hostId, e.getMessage());
        }
        return null;
    }
}
