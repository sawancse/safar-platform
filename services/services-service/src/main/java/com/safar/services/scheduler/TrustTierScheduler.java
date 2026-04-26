package com.safar.services.scheduler;

import com.safar.services.entity.ServiceListing;
import com.safar.services.entity.enums.CommissionTier;
import com.safar.services.entity.enums.ServiceListingStatus;
import com.safar.services.entity.enums.ServiceListingType;
import com.safar.services.repository.ServiceListingRepository;
import com.safar.services.service.CommissionRateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Nightly recompute of {@code service_listings.trust_tier} based on bookings
 * and ratings (JustDial dual-gate pattern — KYC verified PLUS performance
 * threshold). Runs at 02:00 IST every night.
 *
 * Tier ladder:
 *   LISTED          (default — KYC verified, status=VERIFIED)
 *   SAFAR_VERIFIED  + 1+ completed booking + ≥4.0★ + ≥3 reviews
 *   TOP_RATED       + 10+ bookings + ≥4.5★ + 90%+ on-time + 90%+ response
 *
 * On-time and response-rate thresholds for TOP_RATED are not yet computed —
 * we ladder up to TOP_RATED on bookings + rating only for now; the missing
 * thresholds become a no-op gate (always passes), tightened once we wire
 * those metrics.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TrustTierScheduler {

    private final ServiceListingRepository repo;
    private final CommissionRateService commissionRates;

    private static final BigDecimal SAFAR_VERIFIED_RATING = new BigDecimal("4.0");
    private static final int        SAFAR_VERIFIED_REVIEWS = 3;
    private static final int        SAFAR_VERIFIED_BOOKINGS = 1;

    private static final BigDecimal TOP_RATED_RATING = new BigDecimal("4.5");
    private static final int        TOP_RATED_BOOKINGS = 10;

    /** Every night at 02:00 IST (Asia/Kolkata default JVM TZ on prod). */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void recomputeAll() {
        List<ServiceListing> verified = repo.findByStatus(ServiceListingStatus.VERIFIED);
        int trustChanged = 0, commissionPromoted = 0, unchanged = 0;
        for (ServiceListing l : verified) {
            boolean changed = false;

            // Trust tier (rating + booking thresholds — JustDial dual-gate)
            String previousTrust = l.getTrustTier();
            String nextTrust = computeTier(l);
            if (!nextTrust.equals(previousTrust)) {
                l.setTrustTier(nextTrust);
                changed = true;
                trustChanged++;
            }

            // Commission tier (per-service-type promotion — never demotes auto-set)
            String previousComm = l.getCommissionTier();
            String nextComm = computeCommissionTier(l, previousComm);
            if (!nextComm.equals(previousComm)) {
                l.setCommissionTier(nextComm);
                changed = true;
                commissionPromoted++;
            }

            if (changed) repo.save(l);
            else unchanged++;
        }
        log.info("Listing tier recompute: scanned={}, trust_tier_changed={}, commission_promoted={}, unchanged={}",
                verified.size(), trustChanged, commissionPromoted, unchanged);
    }

    /**
     * Decide the right commission tier from completed_bookings_count, but never
     * auto-demote — vendors who got a tier keep it even if a booking is later
     * cancelled or refunded. Demotions go through admin override only.
     */
    private String computeCommissionTier(ServiceListing l, String currentStr) {
        int bookings = l.getCompletedBookingsCount() == null ? 0 : l.getCompletedBookingsCount();
        CommissionTier qualifying = commissionRates.qualifyingTier(l.getServiceType(), bookings);

        CommissionTier current;
        try { current = CommissionTier.valueOf(currentStr); }
        catch (Exception e) { return qualifying.name(); }

        // Promote only — never auto-demote
        return qualifying.ordinal() > current.ordinal() ? qualifying.name() : currentStr;
    }

    private String computeTier(ServiceListing l) {
        BigDecimal avg = l.getAvgRating() == null ? BigDecimal.ZERO : l.getAvgRating();
        int reviews = l.getRatingCount() == null ? 0 : l.getRatingCount();
        int bookings = l.getCompletedBookingsCount() == null ? 0 : l.getCompletedBookingsCount();

        if (bookings >= TOP_RATED_BOOKINGS && avg.compareTo(TOP_RATED_RATING) >= 0) {
            return "TOP_RATED";
        }
        if (bookings >= SAFAR_VERIFIED_BOOKINGS && reviews >= SAFAR_VERIFIED_REVIEWS
                && avg.compareTo(SAFAR_VERIFIED_RATING) >= 0) {
            return "SAFAR_VERIFIED";
        }
        return "LISTED";
    }

    private int tierIndex(String tier) {
        return switch (tier) {
            case "TOP_RATED" -> 2;
            case "SAFAR_VERIFIED" -> 1;
            default -> 0;
        };
    }
}
