package com.safar.user.service;

import com.safar.user.entity.Referral;
import com.safar.user.entity.UserProfile;
import com.safar.user.entity.enums.ReferralStatus;
import com.safar.user.entity.enums.ReferralType;
import com.safar.user.repository.ReferralRepository;
import com.safar.user.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReferralService {

    private final ReferralRepository referralRepository;
    private final ProfileRepository userProfileRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    /**
     * Generate a unique referral code for a user.
     * Called on user profile creation or when user requests their code.
     */
    @Transactional
    public String getOrCreateReferralCode(UUID userId) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        if (profile.getReferralCode() != null) {
            return profile.getReferralCode();
        }

        String code = generateUniqueCode();
        profile.setReferralCode(code);
        userProfileRepository.save(profile);
        return code;
    }

    /**
     * Apply a referral code during sign-up.
     * Called when a new user enters a referral code.
     */
    @Transactional
    public Referral applyReferralCode(String code, UUID newUserId) {
        Referral referral = referralRepository.findByReferralCode(code)
                .orElseThrow(() -> new RuntimeException("Invalid referral code: " + code));

        if (referral.getReferredId() != null) {
            throw new RuntimeException("Referral code already used");
        }
        if (referral.getReferrerId().equals(newUserId)) {
            throw new RuntimeException("Cannot refer yourself");
        }

        referral.setReferredId(newUserId);
        referral.setStatus(ReferralStatus.SIGNED_UP);
        referralRepository.save(referral);

        // Credit the referred user immediately (sign-up bonus)
        referral.setReferredCredited(true);
        referralRepository.save(referral);

        // Update referred user's profile
        userProfileRepository.findByUserId(newUserId).ifPresent(p -> {
            p.setReferredByCode(code);
            userProfileRepository.save(p);
        });

        kafkaTemplate.send("referral.signup", referral.getId().toString(), Map.of(
                "referrerId", referral.getReferrerId(),
                "referredId", newUserId,
                "referredRewardPaise", referral.getReferredRewardPaise()
        ));

        log.info("Referral {} applied: {} referred {}", code, referral.getReferrerId(), newUserId);
        return referral;
    }

    /**
     * Complete a referral when the qualifying action is done.
     * For GUEST referrals: first booking completed.
     * For HOST referrals: first hosted stay completed.
     */
    @Transactional
    public void completeReferral(UUID referredUserId, UUID bookingId) {
        referralRepository.findByReferredId(referredUserId).ifPresent(referral -> {
            if (referral.getStatus() != ReferralStatus.SIGNED_UP) return;

            referral.setStatus(ReferralStatus.COMPLETED);
            referral.setQualifyingBookingId(bookingId);
            referral.setCompletedAt(OffsetDateTime.now());
            referral.setReferrerCredited(true);
            referralRepository.save(referral);

            // Update referrer stats
            userProfileRepository.findByUserId(referral.getReferrerId()).ifPresent(p -> {
                p.setTotalReferrals((p.getTotalReferrals() != null ? p.getTotalReferrals() : 0) + 1);
                p.setReferralEarningsPaise(
                        (p.getReferralEarningsPaise() != null ? p.getReferralEarningsPaise() : 0L) +
                                referral.getReferrerRewardPaise());
                userProfileRepository.save(p);
            });

            kafkaTemplate.send("referral.completed", referral.getId().toString(), Map.of(
                    "referrerId", referral.getReferrerId(),
                    "referrerRewardPaise", referral.getReferrerRewardPaise(),
                    "bookingId", bookingId
            ));

            log.info("Referral completed: {} earned ₹{}", referral.getReferrerId(),
                    referral.getReferrerRewardPaise() / 100);
        });
    }

    /**
     * Create a shareable referral entry (pre-generated for sharing).
     */
    @Transactional
    public Referral createReferral(UUID referrerId, ReferralType type) {
        String code = getOrCreateReferralCode(referrerId);

        Referral referral = Referral.builder()
                .referrerId(referrerId)
                .referralCode(code)
                .type(type)
                .referrerRewardPaise(type == ReferralType.HOST ? 100000L : 50000L) // ₹1000 for host, ₹500 for guest
                .referredRewardPaise(type == ReferralType.HOST ? 50000L : 25000L)
                .build();

        return referralRepository.save(referral);
    }

    public List<Referral> getUserReferrals(UUID userId) {
        return referralRepository.findByReferrerId(userId);
    }

    public Map<String, Object> getReferralStats(UUID userId) {
        long total = referralRepository.findByReferrerId(userId).size();
        long completed = referralRepository.countByReferrerIdAndStatus(userId, ReferralStatus.COMPLETED);
        long pending = referralRepository.countByReferrerIdAndStatus(userId, ReferralStatus.SIGNED_UP);

        UserProfile profile = userProfileRepository.findByUserId(userId).orElse(null);
        long earnings = profile != null && profile.getReferralEarningsPaise() != null
                ? profile.getReferralEarningsPaise() : 0L;

        return Map.of(
                "totalReferrals", total,
                "completed", completed,
                "pending", pending,
                "totalEarningsPaise", earnings,
                "referralCode", profile != null && profile.getReferralCode() != null
                        ? profile.getReferralCode() : getOrCreateReferralCode(userId)
        );
    }

    private String generateUniqueCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder sb = new StringBuilder("SAF");
            for (int i = 0; i < 6; i++) {
                sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
            }
            String code = sb.toString();
            if (referralRepository.findByReferralCode(code).isEmpty()) {
                return code;
            }
        }
        throw new RuntimeException("Failed to generate unique referral code");
    }
}
