package com.safar.user.service;

import com.safar.user.entity.LoyaltyTransaction;
import com.safar.user.entity.UserProfile;
import com.safar.user.entity.enums.LoyaltyTier;
import com.safar.user.repository.LoyaltyTransactionRepository;
import com.safar.user.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoyaltyService {

    private final ProfileRepository userProfileRepository;
    private final LoyaltyTransactionRepository transactionRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // Points earned per ₹100 spent
    private static final long POINTS_PER_100_INR = 10;

    // Tier thresholds
    private static final int SILVER_STAYS = 2;
    private static final int GOLD_STAYS = 5;
    private static final int PLATINUM_STAYS = 15;

    // Discount percentages per tier
    public static final Map<LoyaltyTier, Integer> TIER_DISCOUNTS = Map.of(
            LoyaltyTier.BRONZE, 0,
            LoyaltyTier.SILVER, 5,
            LoyaltyTier.GOLD, 10,
            LoyaltyTier.PLATINUM, 15
    );

    /**
     * Award points after a booking is completed.
     * Called by Kafka consumer when booking.completed event fires.
     */
    @Transactional
    public void awardBookingPoints(UUID userId, UUID bookingId, long amountPaise) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        // Calculate points: 10 points per ₹100
        long points = (amountPaise / 10000) * POINTS_PER_100_INR;
        if (points <= 0) points = 1; // minimum 1 point

        profile.setLoyaltyPoints((profile.getLoyaltyPoints() != null ? profile.getLoyaltyPoints() : 0L) + points);
        profile.setCompletedStays((profile.getCompletedStays() != null ? profile.getCompletedStays() : 0) + 1);

        // Check for tier upgrade
        LoyaltyTier oldTier = profile.getLoyaltyTier() != null
                ? LoyaltyTier.valueOf(profile.getLoyaltyTier()) : LoyaltyTier.BRONZE;
        LoyaltyTier newTier = calculateTier(profile.getCompletedStays());

        if (newTier.ordinal() > oldTier.ordinal()) {
            profile.setLoyaltyTier(newTier.name());
            profile.setTierUpgradedAt(OffsetDateTime.now());
            kafkaTemplate.send("loyalty.tier.upgraded", userId.toString(), Map.of(
                    "userId", userId,
                    "oldTier", oldTier.name(),
                    "newTier", newTier.name(),
                    "discount", TIER_DISCOUNTS.get(newTier)
            ));
            log.info("User {} upgraded from {} to {}", userId, oldTier, newTier);
        }

        userProfileRepository.save(profile);

        // Record transaction
        transactionRepository.save(LoyaltyTransaction.builder()
                .userId(userId)
                .points(points)
                .type("EARN")
                .description("Booking completed — earned " + points + " points")
                .bookingId(bookingId)
                .build());

        log.info("Awarded {} points to user {} for booking {}", points, userId, bookingId);
    }

    /**
     * Get loyalty discount applicable for a user.
     * Called by booking-service when calculating price.
     */
    public int getDiscountPercent(UUID userId) {
        return userProfileRepository.findByUserId(userId)
                .map(p -> {
                    LoyaltyTier tier = p.getLoyaltyTier() != null
                            ? LoyaltyTier.valueOf(p.getLoyaltyTier()) : LoyaltyTier.BRONZE;
                    return TIER_DISCOUNTS.getOrDefault(tier, 0);
                })
                .orElse(0);
    }

    public Map<String, Object> getLoyaltyStatus(UUID userId) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        LoyaltyTier tier = profile.getLoyaltyTier() != null
                ? LoyaltyTier.valueOf(profile.getLoyaltyTier()) : LoyaltyTier.BRONZE;
        int stays = profile.getCompletedStays() != null ? profile.getCompletedStays() : 0;
        long points = profile.getLoyaltyPoints() != null ? profile.getLoyaltyPoints() : 0L;

        // Calculate progress to next tier
        int nextTierStays = switch (tier) {
            case BRONZE -> SILVER_STAYS;
            case SILVER -> GOLD_STAYS;
            case GOLD -> PLATINUM_STAYS;
            case PLATINUM -> PLATINUM_STAYS; // already max
        };

        return Map.of(
                "tier", tier.name(),
                "discount", TIER_DISCOUNTS.get(tier),
                "completedStays", stays,
                "loyaltyPoints", points,
                "nextTier", tier == LoyaltyTier.PLATINUM ? "MAX" : calculateTier(nextTierStays).name(),
                "staysToNextTier", Math.max(0, nextTierStays - stays),
                "benefits", getTierBenefits(tier)
        );
    }

    public Page<LoyaltyTransaction> getTransactions(UUID userId, Pageable pageable) {
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Transactional
    public void redeemPoints(UUID userId, long points, String description) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        long current = profile.getLoyaltyPoints() != null ? profile.getLoyaltyPoints() : 0L;
        if (current < points) {
            throw new RuntimeException("Insufficient points. Have: " + current + ", need: " + points);
        }

        profile.setLoyaltyPoints(current - points);
        userProfileRepository.save(profile);

        transactionRepository.save(LoyaltyTransaction.builder()
                .userId(userId)
                .points(-points)
                .type("REDEEM")
                .description(description)
                .build());
    }

    private LoyaltyTier calculateTier(int completedStays) {
        if (completedStays >= PLATINUM_STAYS) return LoyaltyTier.PLATINUM;
        if (completedStays >= GOLD_STAYS) return LoyaltyTier.GOLD;
        if (completedStays >= SILVER_STAYS) return LoyaltyTier.SILVER;
        return LoyaltyTier.BRONZE;
    }

    private Map<String, Object> getTierBenefits(LoyaltyTier tier) {
        return switch (tier) {
            case BRONZE -> Map.of("discount", 0, "freeCancellation", false, "roomUpgrade", false, "prioritySupport", false);
            case SILVER -> Map.of("discount", 5, "freeCancellation", false, "roomUpgrade", false, "prioritySupport", false);
            case GOLD -> Map.of("discount", 10, "freeCancellation", true, "roomUpgrade", false, "prioritySupport", true);
            case PLATINUM -> Map.of("discount", 15, "freeCancellation", true, "roomUpgrade", true, "prioritySupport", true);
        };
    }
}
