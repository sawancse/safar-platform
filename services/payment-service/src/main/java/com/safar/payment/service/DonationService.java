package com.safar.payment.service;

import com.safar.payment.dto.*;
import com.safar.payment.entity.Donation;
import com.safar.payment.entity.DonationStats;
import com.safar.payment.entity.enums.DonationFrequency;
import com.safar.payment.entity.enums.DonationStatus;
import com.safar.payment.repository.DonationRepository;
import com.safar.payment.repository.DonationStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class DonationService {

    private final RazorpayGateway razorpayGateway;
    private final DonationRepository donationRepo;
    private final DonationStatsRepository statsRepo;
    private final KafkaTemplate<String, String> kafka;

    @Value("${razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${razorpay.webhook-secret}")
    private String webhookSecret;

    private static final AtomicLong SEQ = new AtomicLong(System.currentTimeMillis() % 10000);

    @Transactional
    public DonationOrderResponse createDonation(UUID donorId, CreateDonationRequest req) throws Exception {
        DonationFrequency freq = DonationFrequency.ONE_TIME;
        if ("MONTHLY".equalsIgnoreCase(req.frequency())) {
            freq = DonationFrequency.MONTHLY;
        }

        String donationRef = generateDonationRef();

        Donation donation = Donation.builder()
                .donationRef(donationRef)
                .donorId(donorId)
                .donorName(req.donorName())
                .donorEmail(req.donorEmail())
                .donorPhone(req.donorPhone())
                .donorPan(req.donorPan())
                .amountPaise(req.amountPaise())
                .frequency(freq)
                .dedicatedTo(req.dedicatedTo())
                .dedicationMessage(req.dedicationMessage())
                .campaignCode(req.campaignCode())
                .status(DonationStatus.CREATED)
                .build();

        if (freq == DonationFrequency.MONTHLY) {
            // Create Razorpay subscription for recurring donations
            String subscriptionId = razorpayGateway.createSubscription(
                    "Aashray-Monthly", req.amountPaise());
            donation.setRazorpaySubscriptionId(subscriptionId);
            donationRepo.save(donation);

            return new DonationOrderResponse(
                    donationRef, null, subscriptionId,
                    req.amountPaise(), "INR", razorpayKeyId, "MONTHLY");
        } else {
            // Create Razorpay order for one-time donation
            String orderId = razorpayGateway.createOrder(req.amountPaise(), donationRef);
            donation.setRazorpayOrderId(orderId);
            donationRepo.save(donation);

            return new DonationOrderResponse(
                    donationRef, orderId, null,
                    req.amountPaise(), "INR", razorpayKeyId, "ONE_TIME");
        }
    }

    @Transactional
    public DonationResponse verifyDonation(VerifyDonationRequest req) {
        if (!razorpayGateway.verifyPaymentSignature(
                req.razorpayOrderId(), req.razorpayPaymentId(), req.razorpaySignature())) {
            throw new SecurityException("Invalid donation payment signature");
        }

        Donation donation = donationRepo.findByRazorpayOrderId(req.razorpayOrderId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Donation not found for order: " + req.razorpayOrderId()));

        if (donation.getStatus() == DonationStatus.CAPTURED) {
            return toResponse(donation);
        }

        donation.setRazorpayPaymentId(req.razorpayPaymentId());
        donation.setStatus(DonationStatus.CAPTURED);
        donation.setCapturedAt(OffsetDateTime.now());
        donation.setReceiptNumber(generateReceiptNumber());
        donationRepo.save(donation);

        // Update materialized stats
        updateStats(donation);

        // Publish rich Kafka event for notification service (80G receipt email)
        String donationEvent = String.format(
            "{\"donationRef\":\"%s\",\"donorName\":\"%s\",\"donorEmail\":\"%s\",\"amountPaise\":%d,\"frequency\":\"%s\",\"receiptNumber\":\"%s\",\"donorPan\":\"%s\",\"dedicatedTo\":\"%s\"}",
            donation.getDonationRef(),
            donation.getDonorName() != null ? donation.getDonorName().replace("\"", "\\\"") : "",
            donation.getDonorEmail() != null ? donation.getDonorEmail() : "",
            donation.getAmountPaise(),
            donation.getFrequency().name(),
            donation.getReceiptNumber() != null ? donation.getReceiptNumber() : "",
            donation.getDonorPan() != null ? donation.getDonorPan() : "",
            donation.getDedicatedTo() != null ? donation.getDedicatedTo().replace("\"", "\\\"") : ""
        );
        try {
            kafka.send("donation.captured", donationEvent).get();
        } catch (Exception e) {
            log.error("CRITICAL: Failed to publish donation.captured for {} (₹{}) — 80G email NOT sent. Manual follow-up needed. Error: {}",
                    donation.getDonationRef(), donation.getAmountPaise() / 100, e.getMessage());
        }
        log.info("Donation captured: {} — ₹{} from {}",
                donation.getDonationRef(),
                donation.getAmountPaise() / 100,
                donation.getDonorName() != null ? donation.getDonorName() : "Anonymous");

        return toResponse(donation);
    }

    public DonationStatsResponse getStats() {
        DonationStats stats = statsRepo.findAll().stream().findFirst()
                .orElse(DonationStats.builder()
                        .totalRaisedPaise(0L)
                        .goalPaise(50000000L)
                        .totalDonors(0)
                        .familiesHoused(0)
                        .monthlyDonors(0)
                        .build());

        // Fetch recent donors for social proof ticker
        List<Donation> recent = donationRepo.findRecentDonations(PageRequest.of(0, 5));
        List<DonationStatsResponse.RecentDonor> recentDonors = recent.stream()
                .map(d -> new DonationStatsResponse.RecentDonor(
                        d.getDonorName() != null ? maskName(d.getDonorName()) : "Anonymous",
                        d.getAmountPaise(),
                        null,
                        Duration.between(d.getCapturedAt(), OffsetDateTime.now()).toMinutes()
                ))
                .toList();

        int progressPercent = stats.getGoalPaise() > 0
                ? (int) (stats.getTotalRaisedPaise() * 100 / stats.getGoalPaise())
                : 0;

        return new DonationStatsResponse(
                stats.getTotalRaisedPaise(),
                stats.getGoalPaise(),
                stats.getTotalDonors(),
                stats.getFamiliesHoused(),
                stats.getMonthlyDonors(),
                Math.min(progressPercent, 100),
                stats.getActiveCampaign(),
                stats.getCampaignTagline(),
                recentDonors
        );
    }

    public Page<DonationResponse> getMyDonations(UUID donorId, Pageable pageable) {
        return donationRepo.findByDonorIdOrderByCreatedAtDesc(donorId, pageable)
                .map(this::toResponse);
    }

    public Page<DonationResponse> getAllDonations(DonationStatus status, Pageable pageable) {
        if (status != null) {
            return donationRepo.findByStatusOrderByCreatedAtDesc(status, pageable)
                    .map(this::toResponse);
        }
        return donationRepo.findAll(pageable).map(this::toResponse);
    }

    public DonationResponse getDonation(String donationRef) {
        return donationRepo.findByDonationRef(donationRef)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Donation not found: " + donationRef));
    }

    public DonorLeaderboardResponse getLeaderboard() {
        // Top donors (anonymized, opt-in would be Phase 3)
        List<Donation> captured = donationRepo.findRecentDonations(PageRequest.of(0, 100));

        // Aggregate by donor name
        Map<String, Long> donorTotals = new java.util.LinkedHashMap<>();
        Map<String, Integer> donorCounts = new java.util.LinkedHashMap<>();
        for (Donation d : captured) {
            String name = d.getDonorName() != null ? maskName(d.getDonorName()) : "Anonymous";
            donorTotals.merge(name, d.getAmountPaise(), Long::sum);
            donorCounts.merge(name, 1, Integer::sum);
        }

        List<DonorLeaderboardResponse.LeaderboardEntry> topDonors = donorTotals.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .map(e -> new DonorLeaderboardResponse.LeaderboardEntry(
                        e.getKey(), e.getValue(), donorCounts.getOrDefault(e.getKey(), 1),
                        determineTier(e.getValue())))
                .toList();

        return new DonorLeaderboardResponse(topDonors, List.of(), "all-time");
    }

    private String determineTier(long totalPaise) {
        long rupees = totalPaise / 100;
        if (rupees >= 15000) return "Patron";
        if (rupees >= 5000) return "Champion";
        if (rupees >= 2000) return "Builder";
        if (rupees >= 500) return "Friend";
        return "Supporter";
    }

    // ── Private helpers ──────────────────────────────────────────

    private void updateStats(Donation donation) {
        DonationStats stats = statsRepo.findAll().stream().findFirst()
                .orElse(DonationStats.builder()
                        .totalRaisedPaise(0L)
                        .goalPaise(50000000L)
                        .totalDonors(0)
                        .familiesHoused(0)
                        .monthlyDonors(0)
                        .build());

        stats.setTotalRaisedPaise(stats.getTotalRaisedPaise() + donation.getAmountPaise());
        stats.setTotalDonors(donationRepo.countUniqueDonors());
        if (donation.getFrequency() == DonationFrequency.MONTHLY) {
            stats.setMonthlyDonors(stats.getMonthlyDonors() + 1);
        }
        // Rough estimate: ₹10,000 = 1 family-month of housing
        stats.setFamiliesHoused((int) (stats.getTotalRaisedPaise() / 1000000L));
        statsRepo.save(stats);
    }

    private String maskName(String name) {
        if (name.length() <= 2) return name;
        return name.charAt(0) + "***" + name.charAt(name.length() - 1);
    }

    private String generateDonationRef() {
        return "DON-" + java.time.Year.now().getValue() + "-" + SEQ.incrementAndGet();
    }

    private String generateReceiptNumber() {
        return "80G-" + java.time.Year.now().getValue() + "-" + SEQ.incrementAndGet();
    }

    private DonationResponse toResponse(Donation d) {
        return new DonationResponse(
                d.getId().toString(),
                d.getDonationRef(),
                d.getAmountPaise(),
                d.getCurrency(),
                d.getFrequency().name(),
                d.getStatus().name(),
                d.getDonorName(),
                d.getDonorEmail(),
                d.getDedicatedTo(),
                d.getDedicationMessage(),
                d.getReceiptNumber(),
                d.getCampaignCode(),
                d.getCapturedAt(),
                d.getCreatedAt()
        );
    }
}
