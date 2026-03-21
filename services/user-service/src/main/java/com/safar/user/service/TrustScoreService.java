package com.safar.user.service;

import com.safar.user.repository.HostKycRepository;
import com.safar.user.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrustScoreService {

    private final ProfileRepository profileRepository;
    private final HostKycRepository kycRepository;

    @Transactional
    public int calculateTrustScore(UUID userId) {
        var profile = profileRepository.findById(userId).orElse(null);
        var kyc = kycRepository.findByUserId(userId).orElse(null);

        int score = 0;

        // Identity verified: +30
        if (kyc != null && (Boolean.TRUE.equals(kyc.getAadhaarVerified()) || Boolean.TRUE.equals(kyc.getPanVerified()))) {
            score += 30;
        }

        // Selfie matched: +15
        if (profile != null && Boolean.TRUE.equals(profile.getSelfieVerified())) {
            score += 15;
        }

        // Bank verified: +15
        if (kyc != null && Boolean.TRUE.equals(kyc.getBankVerified())) {
            score += 15;
        }

        // DigiLocker verified: +10 (bonus on top of identity)
        if (profile != null && Boolean.TRUE.equals(profile.getDigilockerVerified())) {
            score += 10;
        }

        // Profile completeness: +10
        if (profile != null && profile.getName() != null && profile.getEmail() != null) {
            score += 10;
        }

        // KYC fully verified: +10
        if (kyc != null && "VERIFIED".equals(kyc.getStatus().name())) {
            score += 10;
        }

        // Business verified (GSTIN): +5
        if (kyc != null && Boolean.TRUE.equals(kyc.getGstVerified())) {
            score += 5;
        }

        // Cap at 100
        score = Math.min(100, score);

        // Update profile
        if (profile != null) {
            profile.setTrustScore(score);
            // Update verification level based on score
            if (score >= 70) {
                profile.setVerificationLevel("VERIFIED");
            } else if (score >= 30) {
                profile.setVerificationLevel("BASIC");
            } else {
                profile.setVerificationLevel("UNVERIFIED");
            }

            // Set payout cap based on level
            if ("VERIFIED".equals(profile.getVerificationLevel())) {
                profile.setMonthlyPayoutCapPaise(50000000L); // Rs 5,00,000
            } else if ("BASIC".equals(profile.getVerificationLevel())) {
                profile.setMonthlyPayoutCapPaise(1000000L); // Rs 10,000
            } else {
                profile.setMonthlyPayoutCapPaise(0L);
            }

            profileRepository.save(profile);
            log.info("Trust score updated for user {}: score={}, level={}", userId, score, profile.getVerificationLevel());
        }

        return score;
    }

    public String getTrustBadge(int score) {
        if (score >= 90) return "SUPERHOST";
        if (score >= 70) return "VERIFIED";
        if (score >= 50) return "TRUSTED";
        if (score >= 30) return "ID_VERIFIED";
        return "NEW";
    }
}
