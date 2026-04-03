package com.safar.chef.service;

import com.safar.chef.entity.ChefProfile;
import com.safar.chef.entity.ChefReferral;
import com.safar.chef.repository.ChefProfileRepository;
import com.safar.chef.repository.ChefReferralRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChefReferralService {

    private final ChefReferralRepository referralRepo;
    private final ChefProfileRepository chefProfileRepo;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long REFERRAL_BONUS_PAISE = 50000L; // ₹500

    public String generateReferralCode(UUID userId) {
        ChefProfile chef = chefProfileRepo.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Chef profile not found"));

        if (chef.getReferralCode() != null) return chef.getReferralCode();

        String code = "CHEF-" + String.format("%06d", RANDOM.nextInt(999999));
        chef.setReferralCode(code);
        chefProfileRepo.save(chef);
        return code;
    }

    @Transactional
    public void applyReferral(UUID newChefId, String referralCode) {
        if (referralCode == null || referralCode.isBlank()) return;

        ChefProfile referrer = chefProfileRepo.findAll().stream()
                .filter(c -> referralCode.equals(c.getReferralCode()))
                .findFirst().orElse(null);
        if (referrer == null) {
            log.warn("Invalid referral code: {}", referralCode);
            return;
        }

        if (referralRepo.existsByReferredChefId(newChefId)) {
            log.warn("Chef {} already has a referrer", newChefId);
            return;
        }

        ChefProfile newChef = chefProfileRepo.findById(newChefId)
                .orElseThrow(() -> new IllegalArgumentException("New chef not found"));
        newChef.setReferredBy(referrer.getId());
        chefProfileRepo.save(newChef);

        ChefReferral referral = ChefReferral.builder()
                .referrerId(referrer.getId())
                .referredChefId(newChefId)
                .bonusPaise(REFERRAL_BONUS_PAISE)
                .status("PENDING")
                .build();
        referralRepo.save(referral);

        referrer.setReferralCount(referrer.getReferralCount() + 1);
        referrer.setReferralEarningsPaise(referrer.getReferralEarningsPaise() + REFERRAL_BONUS_PAISE);
        chefProfileRepo.save(referrer);

        log.info("Referral applied: referrer={} newChef={} bonus=₹{}", referrer.getId(), newChefId, REFERRAL_BONUS_PAISE / 100);
    }

    @Transactional(readOnly = true)
    public List<ChefReferral> getMyReferrals(UUID userId) {
        ChefProfile chef = chefProfileRepo.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Chef profile not found"));
        return referralRepo.findByReferrerId(chef.getId());
    }
}
