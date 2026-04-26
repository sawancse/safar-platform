package com.safar.services.service;

import com.safar.services.entity.ChefProfile;
import com.safar.services.repository.ChefProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChefBadgeService {

    private final ChefProfileRepository chefProfileRepo;

    // Badge tiers:
    // TOP_CHEF: rating >= 4.8, totalBookings >= 50, completionRate >= 95%
    // RISING_STAR: rating >= 4.5, totalBookings >= 10, completionRate >= 90%
    // TOP_10: in top 10 by rating (minimum 20 bookings)
    // VERIFIED_PRO: verified + foodSafetyCertificate

    @Scheduled(cron = "0 0 3 * * *") // 3 AM daily
    @Transactional
    public void recalculateBadges() {
        log.info("Recalculating chef badges...");
        List<ChefProfile> chefs = chefProfileRepo.findAll();

        // Find top 10 by rating (min 20 bookings)
        List<ChefProfile> top10 = chefs.stream()
                .filter(c -> c.getTotalBookings() >= 20)
                .sorted((a, b) -> Double.compare(b.getRating(), a.getRating()))
                .limit(10)
                .toList();

        for (ChefProfile chef : chefs) {
            String newBadge = calculateBadge(chef, top10);
            if (newBadge != null && !newBadge.equals(chef.getBadge())) {
                chef.setBadge(newBadge);
                chef.setBadgeAwardedAt(OffsetDateTime.now());
                chefProfileRepo.save(chef);
                log.info("Chef {} awarded badge: {}", chef.getId(), newBadge);
            }
        }
    }

    private String calculateBadge(ChefProfile chef, List<ChefProfile> top10) {
        // Highest tier first
        if (chef.getRating() >= 4.8 && chef.getTotalBookings() >= 50 && chef.getCompletionRate() >= 95.0) {
            return "TOP_CHEF";
        }
        if (top10.contains(chef)) {
            return "TOP_10";
        }
        if (chef.getRating() >= 4.5 && chef.getTotalBookings() >= 10 && chef.getCompletionRate() >= 90.0) {
            return "RISING_STAR";
        }
        if (Boolean.TRUE.equals(chef.getVerified()) && Boolean.TRUE.equals(chef.getFoodSafetyCertificate())) {
            return "VERIFIED_PRO";
        }
        return chef.getBadge(); // keep existing
    }
}
