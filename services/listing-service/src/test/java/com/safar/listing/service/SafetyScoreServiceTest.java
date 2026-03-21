package com.safar.listing.service;

import com.safar.listing.dto.SafetyScoreDto;
import com.safar.listing.entity.Listing;
import com.safar.listing.entity.enums.ListingType;
import com.safar.listing.entity.enums.PricingUnit;
import com.safar.listing.repository.ListingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SafetyScoreServiceTest {

    @Mock
    ListingRepository listingRepository;

    @InjectMocks
    SafetyScoreService safetyScoreService;

    private final UUID listingId = UUID.randomUUID();

    @Test
    void computeScore_knownCity_usesCityBaseline() {
        SafetyScoreDto score = safetyScoreService.computeScore(listingId, "Pune");

        // Pune baseline = 70, crimeScore=70, reviewScore=75, amenityScore=73
        // overall = 70*0.4 + 75*0.3 + 73*0.3 = 28 + 22.5 + 21.9 = 72.4
        assertThat(score.listingId()).isEqualTo(listingId);
        assertThat(score.crimeScore()).isEqualTo(70.0);
        assertThat(score.overallScore()).isEqualTo(72.4);
        assertThat(score.womenFriendly()).isTrue();
    }

    @Test
    void computeScore_unknownCity_usesDefaultBaseline() {
        SafetyScoreDto score = safetyScoreService.computeScore(listingId, "Jaipur");

        // Default baseline = 60, crimeScore=60, reviewScore=65, amenityScore=63
        // overall = 60*0.4 + 65*0.3 + 63*0.3 = 24 + 19.5 + 18.9 = 62.4
        assertThat(score.overallScore()).isEqualTo(62.4);
        assertThat(score.crimeScore()).isEqualTo(60.0);
        assertThat(score.womenFriendly()).isFalse();
    }

    @Test
    void computeScore_labelAssignment() {
        // Pune (70 baseline) -> overall 72.4 -> SAFE
        SafetyScoreDto puneScore = safetyScoreService.computeScore(listingId, "Pune");
        assertThat(puneScore.label()).isEqualTo("SAFE");

        // Delhi (48 baseline) -> overall 48*0.4 + 53*0.3 + 51*0.3 = 19.2 + 15.9 + 15.3 = 50.4 -> MODERATE
        SafetyScoreDto delhiScore = safetyScoreService.computeScore(listingId, "Delhi");
        assertThat(delhiScore.label()).isEqualTo("MODERATE");

        // Direct label assignment checks
        assertThat(SafetyScoreService.assignLabel(85)).isEqualTo("VERY_SAFE");
        assertThat(SafetyScoreService.assignLabel(65)).isEqualTo("SAFE");
        assertThat(SafetyScoreService.assignLabel(50)).isEqualTo("MODERATE");
        assertThat(SafetyScoreService.assignLabel(40)).isEqualTo("CAUTION");
    }
}
