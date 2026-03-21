package com.safar.listing.service;

import com.safar.listing.dto.InvestmentSignalDto;
import com.safar.listing.entity.Listing;
import com.safar.listing.repository.ListingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvestmentSignalService {

    private final ListingRepository listingRepository;

    private static final Map<String, double[]> CITY_SIGNALS = Map.of(
            // avgMonthlyRevenuePaise, yieldPct, occupancyPct
            "goa",       new double[]{450000, 9.5, 78.5},
            "bangalore", new double[]{350000, 7.2, 70.2},
            "mumbai",    new double[]{500000, 5.8, 74.5}
    );
    private static final double[] DEFAULT_SIGNAL = {300000, 6.0, 65.0};

    private static final String DISCLAIMER = "Investment signals are estimates based on historical data " +
            "and market trends. Past performance does not guarantee future returns. " +
            "Please conduct your own due diligence before making investment decisions.";

    public InvestmentSignalDto computeForListing(UUID listingId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new NoSuchElementException("Listing not found: " + listingId));

        String city = listing.getCity() != null ? listing.getCity().toLowerCase() : "";
        double[] signals = CITY_SIGNALS.getOrDefault(city, DEFAULT_SIGNAL);

        long avgMonthlyRevenue = Math.round(signals[0]);
        double annualYield = signals[1];
        double occupancy = signals[2];

        String demandLevel = determineDemandLevel(occupancy);
        String trend = determineTrend(annualYield);
        String confidence = determineConfidence(occupancy, annualYield);

        log.info("Investment signal computed for listing {} in city {}", listingId, listing.getCity());

        return new InvestmentSignalDto(
                avgMonthlyRevenue,
                annualYield,
                occupancy,
                demandLevel,
                trend,
                confidence,
                120,  // stub data point count
                DISCLAIMER
        );
    }

    private String determineDemandLevel(double occupancy) {
        if (occupancy >= 75) return "HIGH";
        if (occupancy >= 60) return "MODERATE";
        return "LOW";
    }

    private String determineTrend(double yield) {
        if (yield >= 8) return "RISING";
        if (yield >= 6) return "STABLE";
        return "DECLINING";
    }

    private String determineConfidence(double occupancy, double yield) {
        if (occupancy >= 70 && yield >= 7) return "HIGH";
        if (occupancy >= 55 && yield >= 5) return "MEDIUM";
        return "LOW";
    }
}
