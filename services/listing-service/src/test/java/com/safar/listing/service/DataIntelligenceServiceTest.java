package com.safar.listing.service;

import com.safar.listing.dto.RentalYieldDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class DataIntelligenceServiceTest {

    @InjectMocks
    DataIntelligenceService dataIntelligenceService;

    @Test
    void getRentalYields_knownCity_returnsCorrectData() {
        RentalYieldDto goaYield = dataIntelligenceService.getRentalYields("goa");

        assertThat(goaYield.city()).isEqualTo("Goa");
        assertThat(goaYield.avgYieldPct()).isEqualTo(9.5);
        assertThat(goaYield.avgMonthlyRentPaise()).isEqualTo(4500000L);
        assertThat(goaYield.trend()).isEqualTo("RISING");

        RentalYieldDto bangaloreYield = dataIntelligenceService.getRentalYields("bangalore");
        assertThat(bangaloreYield.avgYieldPct()).isEqualTo(7.2);

        RentalYieldDto mumbaiYield = dataIntelligenceService.getRentalYields("mumbai");
        assertThat(mumbaiYield.avgYieldPct()).isEqualTo(5.8);
    }

    @Test
    void getRentalYields_unknownCity_returnsDefault() {
        RentalYieldDto yield = dataIntelligenceService.getRentalYields("jaipur");

        assertThat(yield.city()).isEqualTo("jaipur");
        assertThat(yield.avgYieldPct()).isEqualTo(6.0);
        assertThat(yield.avgMonthlyRentPaise()).isEqualTo(3000000L);
        assertThat(yield.trend()).isEqualTo("STABLE");
    }
}
