package com.safar.listing.service;

import com.safar.listing.dto.DemandAlertDto;
import com.safar.listing.entity.Listing;
import com.safar.listing.repository.ListingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DemandAlertService {

    private final ListingRepository listingRepository;

    private static final Map<String, Double> CITY_WEEKEND_MULTIPLIERS = Map.of(
            "goa", 1.5,
            "mumbai", 1.3,
            "bangalore", 1.4,
            "delhi", 1.35
    );
    private static final double DEFAULT_WEEKEND_MULTIPLIER = 1.2;

    private static final Map<String, List<Integer>> PEAK_MONTHS = Map.of(
            "goa", List.of(11, 12, 1, 2),
            "mumbai", List.of(10, 11, 12),
            "bangalore", List.of(9, 10, 11)
    );

    public double computeDemandMultiplier(String city, LocalDate targetDate) {
        double multiplier = 1.0;
        String cityLower = city.toLowerCase();

        // Weekend boost
        DayOfWeek day = targetDate.getDayOfWeek();
        if (day == DayOfWeek.FRIDAY || day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            multiplier *= CITY_WEEKEND_MULTIPLIERS.getOrDefault(cityLower, DEFAULT_WEEKEND_MULTIPLIER);
        }

        // Peak season boost
        int month = targetDate.getMonthValue();
        List<Integer> peakMonths = PEAK_MONTHS.get(cityLower);
        if (peakMonths != null && peakMonths.contains(month)) {
            multiplier *= 1.3;
        }

        return multiplier;
    }

    public String getAlertType(double multiplier) {
        if (multiplier >= 1.4) return "SURGE";
        if (multiplier >= 1.2) return "HIGH_DEMAND";
        if (multiplier < 0.9) return "LOW_DEMAND";
        return "PRICE_OPPORTUNITY";
    }

    public DemandAlertDto computeAlert(UUID listingId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new NoSuchElementException("Listing not found: " + listingId));

        LocalDate tomorrow = LocalDate.now().plusDays(1);
        double maxMultiplier = 1.0;
        LocalDate peakDate = tomorrow;

        for (int i = 0; i < 7; i++) {
            LocalDate date = tomorrow.plusDays(i);
            double m = computeDemandMultiplier(listing.getCity(), date);
            if (m > maxMultiplier) {
                maxMultiplier = m;
                peakDate = date;
            }
        }

        String alertType = getAlertType(maxMultiplier);
        long suggestedPricePaise = Math.round(listing.getBasePricePaise() * maxMultiplier);

        String urgency;
        if (maxMultiplier >= 1.4) urgency = "HIGH";
        else if (maxMultiplier >= 1.2) urgency = "MEDIUM";
        else urgency = "LOW";

        String messageEn = String.format(
                "%s demand expected around %s in %s. Consider adjusting your price to %d paise.",
                alertType, peakDate, listing.getCity(), suggestedPricePaise);

        return new DemandAlertDto(
                listingId,
                alertType,
                messageEn,
                suggestedPricePaise,
                maxMultiplier,
                urgency
        );
    }
}
