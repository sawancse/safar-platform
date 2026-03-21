package com.safar.listing.service;

import com.safar.listing.repository.ListingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class DemandAlertServiceTest {

    @Mock
    ListingRepository listingRepository;

    @InjectMocks
    DemandAlertService demandAlertService;

    @Test
    void computeDemandMultiplier_weekendApplied() {
        // Find next Saturday for a reliable weekend test
        LocalDate saturday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.SATURDAY));

        double multiplier = demandAlertService.computeDemandMultiplier("goa", saturday);

        // Goa weekend multiplier is 1.5
        assertThat(multiplier).isGreaterThanOrEqualTo(1.5);
    }

    @Test
    void computeDemandMultiplier_peakSeasonApplied() {
        // November weekday for Goa (peak season month)
        LocalDate novMonday = LocalDate.of(2026, 11, 2); // 2026-11-02 is a Monday
        // Verify it's actually a weekday
        while (novMonday.getDayOfWeek() == DayOfWeek.SATURDAY
                || novMonday.getDayOfWeek() == DayOfWeek.SUNDAY
                || novMonday.getDayOfWeek() == DayOfWeek.FRIDAY) {
            novMonday = novMonday.plusDays(1);
        }

        double multiplier = demandAlertService.computeDemandMultiplier("goa", novMonday);

        // Peak season adds 1.3x on top of base 1.0 (weekday) = 1.3
        assertThat(multiplier).isEqualTo(1.3);
    }

    @Test
    void getAlertType_surgeForHighMultiplier() {
        assertThat(demandAlertService.getAlertType(1.5)).isEqualTo("SURGE");
        assertThat(demandAlertService.getAlertType(1.4)).isEqualTo("SURGE");
        assertThat(demandAlertService.getAlertType(1.3)).isEqualTo("HIGH_DEMAND");
        assertThat(demandAlertService.getAlertType(0.8)).isEqualTo("LOW_DEMAND");
        assertThat(demandAlertService.getAlertType(1.0)).isEqualTo("PRICE_OPPORTUNITY");
    }
}
