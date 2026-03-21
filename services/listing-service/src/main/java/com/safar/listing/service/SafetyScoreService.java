package com.safar.listing.service;

import com.safar.listing.dto.SafetyScoreDto;
import com.safar.listing.entity.Listing;
import com.safar.listing.repository.ListingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SafetyScoreService {

    private final ListingRepository listingRepository;

    private static final Map<String, Double> CITY_BASELINES = Map.of(
            "mumbai", 62.0,
            "delhi", 48.0,
            "bangalore", 67.0,
            "pune", 70.0
    );
    private static final double DEFAULT_BASELINE = 60.0;

    public SafetyScoreDto computeScore(UUID listingId, String city) {
        double baseline = CITY_BASELINES.getOrDefault(
                city != null ? city.toLowerCase() : "", DEFAULT_BASELINE);

        // Stub sub-scores derived from city baseline
        double crimeScore = baseline;
        double reviewScore = Math.min(baseline + 5, 100);
        double amenityScore = Math.min(baseline + 3, 100);

        double overallScore = (crimeScore * 0.4) + (reviewScore * 0.3) + (amenityScore * 0.3);
        overallScore = Math.round(overallScore * 10.0) / 10.0;

        boolean womenFriendly = overallScore >= 70;
        double womenScore = womenFriendly ? overallScore + 2 : overallScore - 5;
        womenScore = Math.max(0, Math.min(100, Math.round(womenScore * 10.0) / 10.0));

        String label = assignLabel(overallScore);
        String summary = String.format("Neighborhood safety: %s (%.1f/100)", label, overallScore);

        return new SafetyScoreDto(
                listingId, overallScore, crimeScore, reviewScore,
                amenityScore, womenFriendly, womenScore, label, summary
        );
    }

    @Transactional
    public void updateListingSafety(UUID listingId, SafetyScoreDto score) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new NoSuchElementException("Listing not found: " + listingId));
        listing.setSafetyScore(BigDecimal.valueOf(score.overallScore()));
        listing.setSafetyLabel(score.label());
        listing.setWomenFriendly(score.womenFriendly());
        listing.setSafetyUpdatedAt(OffsetDateTime.now());
        listingRepository.save(listing);
        log.info("Safety score updated for listing {}: {} ({})", listingId, score.overallScore(), score.label());
    }

    static String assignLabel(double score) {
        if (score >= 80) return "VERY_SAFE";
        if (score >= 65) return "SAFE";
        if (score >= 50) return "MODERATE";
        return "CAUTION";
    }
}
