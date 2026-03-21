package com.safar.user.service;

import com.safar.user.entity.UserProfile;
import com.safar.user.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Safar Star Host — earned badge similar to Airbnb Superhost.
 *
 * Criteria (all must be met):
 * - Average host rating >= 4.8
 * - Cancellation rate < 2%
 * - 10+ completed stays in last 12 months
 * - Response rate >= 90%
 *
 * Evaluated quarterly. Badge can be earned or lost.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StarHostService {

    private final ProfileRepository profileRepository;

    private static final double MIN_RATING = 4.8;
    private static final double MAX_CANCEL_RATE = 2.0;
    private static final int MIN_COMPLETED_STAYS = 10;
    private static final int MIN_RESPONSE_RATE = 90;

    /**
     * Check if a specific host qualifies for Star Host.
     */
    public boolean qualifies(UserProfile host) {
        if (host.getAvgHostRating() == null || host.getAvgHostRating() < MIN_RATING) return false;
        if (host.getCancellationRatePercent() != null && host.getCancellationRatePercent() >= MAX_CANCEL_RATE) return false;
        if (host.getTotalCompletedStays() == null || host.getTotalCompletedStays() < MIN_COMPLETED_STAYS) return false;
        if (host.getResponseRate() == null || host.getResponseRate() < MIN_RESPONSE_RATE) return false;
        return true;
    }

    /**
     * Evaluate and update Star Host status for a single host.
     */
    @Transactional
    public boolean evaluate(UUID hostId) {
        UserProfile host = profileRepository.findById(hostId).orElse(null);
        if (host == null || !"HOST".equals(host.getRole())) return false;

        boolean wasStarHost = Boolean.TRUE.equals(host.getStarHost());
        boolean nowQualifies = qualifies(host);

        if (nowQualifies && !wasStarHost) {
            host.setStarHost(true);
            host.setStarHostSince(OffsetDateTime.now());
            profileRepository.save(host);
            log.info("⭐ Host {} earned Star Host badge (rating={}, cancelRate={}%, stays={}, responseRate={}%)",
                    hostId, host.getAvgHostRating(), host.getCancellationRatePercent(),
                    host.getTotalCompletedStays(), host.getResponseRate());
            return true;
        } else if (!nowQualifies && wasStarHost) {
            host.setStarHost(false);
            host.setStarHostSince(null);
            profileRepository.save(host);
            log.info("Host {} lost Star Host badge", hostId);
            return false;
        }

        return wasStarHost;
    }

    /**
     * Update host metrics (called via Kafka events from booking/review services).
     */
    @Transactional
    public void updateHostMetrics(UUID hostId, Double avgRating, Double cancelRate, Integer completedStays) {
        UserProfile host = profileRepository.findById(hostId).orElse(null);
        if (host == null) return;

        if (avgRating != null) host.setAvgHostRating(avgRating);
        if (cancelRate != null) host.setCancellationRatePercent(cancelRate);
        if (completedStays != null) host.setTotalCompletedStays(completedStays);

        profileRepository.save(host);
        evaluate(hostId);
    }

    /**
     * Quarterly evaluation: re-check all hosts.
     * Runs on 1st of Jan, Apr, Jul, Oct at 3 AM.
     */
    @Scheduled(cron = "0 0 3 1 1,4,7,10 *")
    public void quarterlyEvaluation() {
        log.info("Starting quarterly Star Host evaluation...");
        List<UserProfile> hosts = profileRepository.findByRole("HOST");

        int earned = 0, lost = 0;
        for (UserProfile host : hosts) {
            boolean wasStarHost = Boolean.TRUE.equals(host.getStarHost());
            boolean nowQualifies = qualifies(host);

            if (nowQualifies && !wasStarHost) {
                host.setStarHost(true);
                host.setStarHostSince(OffsetDateTime.now());
                earned++;
            } else if (!nowQualifies && wasStarHost) {
                host.setStarHost(false);
                host.setStarHostSince(null);
                lost++;
            }
            profileRepository.save(host);
        }

        log.info("Quarterly Star Host evaluation complete: {} earned, {} lost, {} total hosts",
                earned, lost, hosts.size());
    }

    /**
     * Get Star Host criteria for display.
     */
    public record StarHostCriteria(
            double minRating, double maxCancelRate, int minStays, int minResponseRate,
            Double currentRating, Double currentCancelRate, Integer currentStays, Integer currentResponseRate,
            boolean qualifies
    ) {}

    public StarHostCriteria getCriteria(UUID hostId) {
        UserProfile host = profileRepository.findById(hostId).orElse(null);
        if (host == null) {
            return new StarHostCriteria(MIN_RATING, MAX_CANCEL_RATE, MIN_COMPLETED_STAYS, MIN_RESPONSE_RATE,
                    null, null, null, null, false);
        }
        return new StarHostCriteria(
                MIN_RATING, MAX_CANCEL_RATE, MIN_COMPLETED_STAYS, MIN_RESPONSE_RATE,
                host.getAvgHostRating(), host.getCancellationRatePercent(),
                host.getTotalCompletedStays(), host.getResponseRate(),
                qualifies(host)
        );
    }
}
