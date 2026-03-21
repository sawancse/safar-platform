package com.safar.listing.service;

import com.safar.listing.dto.DemandTrendDto;
import com.safar.listing.dto.OccupancyRateDto;
import com.safar.listing.dto.RentalYieldDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataIntelligenceService {

    private static final Map<String, RentalYieldDto> CITY_YIELDS = Map.of(
            "goa", new RentalYieldDto("Goa", 9.5, 4500000L, 56842105L, "RISING"),
            "bangalore", new RentalYieldDto("Bangalore", 7.2, 3500000L, 58333333L, "STABLE"),
            "mumbai", new RentalYieldDto("Mumbai", 5.8, 5000000L, 103448276L, "STABLE")
    );

    private static final RentalYieldDto DEFAULT_YIELD =
            new RentalYieldDto("Unknown", 6.0, 3000000L, 60000000L, "STABLE");

    private static final Map<String, List<OccupancyRateDto>> CITY_OCCUPANCY = Map.of(
            "goa", List.of(
                    new OccupancyRateDto("Goa", "VILLA", 78.5, 850000),
                    new OccupancyRateDto("Goa", "APARTMENT", 72.0, 450000),
                    new OccupancyRateDto("Goa", "HOMESTAY", 68.3, 300000)
            ),
            "bangalore", List.of(
                    new OccupancyRateDto("Bangalore", "VILLA", 65.0, 600000),
                    new OccupancyRateDto("Bangalore", "APARTMENT", 70.2, 350000),
                    new OccupancyRateDto("Bangalore", "HOMESTAY", 62.1, 250000)
            ),
            "mumbai", List.of(
                    new OccupancyRateDto("Mumbai", "VILLA", 60.0, 1200000),
                    new OccupancyRateDto("Mumbai", "APARTMENT", 74.5, 600000),
                    new OccupancyRateDto("Mumbai", "HOMESTAY", 58.0, 400000)
            )
    );

    private static final List<OccupancyRateDto> DEFAULT_OCCUPANCY = List.of(
            new OccupancyRateDto("Unknown", "VILLA", 60.0, 500000),
            new OccupancyRateDto("Unknown", "APARTMENT", 65.0, 300000),
            new OccupancyRateDto("Unknown", "HOMESTAY", 55.0, 200000)
    );

    public RentalYieldDto getRentalYields(String city) {
        String key = city != null ? city.toLowerCase() : "";
        RentalYieldDto yield = CITY_YIELDS.getOrDefault(key, DEFAULT_YIELD);
        if (yield == DEFAULT_YIELD && city != null) {
            // Return with proper city name for unknown cities
            return new RentalYieldDto(city, yield.avgYieldPct(), yield.avgMonthlyRentPaise(),
                    yield.avgPropertyValuePaise(), yield.trend());
        }
        return yield;
    }

    public List<DemandTrendDto> getDemandTrends(String city, int months) {
        String cityName = city != null ? city : "Unknown";
        List<DemandTrendDto> trends = new ArrayList<>();
        YearMonth current = YearMonth.now();

        for (int i = months - 1; i >= 0; i--) {
            YearMonth month = current.minusMonths(i);
            String monthStr = month.toString();

            // Stub: generate realistic-looking data
            long baseSearchVolume = getCityBaseSearchVolume(cityName);
            long searchVolume = baseSearchVolume + (long) (Math.sin(i * 0.5) * baseSearchVolume * 0.2);
            long bookingCount = Math.round(searchVolume * 0.08);
            double occupancy = 60 + Math.sin(i * 0.5) * 15;
            occupancy = Math.round(occupancy * 10.0) / 10.0;

            trends.add(new DemandTrendDto(cityName, monthStr, searchVolume, bookingCount, occupancy));
        }

        return trends;
    }

    public List<OccupancyRateDto> getOccupancyRates(String city) {
        String key = city != null ? city.toLowerCase() : "";
        List<OccupancyRateDto> rates = CITY_OCCUPANCY.getOrDefault(key, DEFAULT_OCCUPANCY);
        if (rates == DEFAULT_OCCUPANCY && city != null) {
            // Return with proper city name for unknown cities
            return rates.stream()
                    .map(r -> new OccupancyRateDto(city, r.propertyType(), r.occupancyPct(), r.avgDailyRatePaise()))
                    .toList();
        }
        return rates;
    }

    private long getCityBaseSearchVolume(String city) {
        return switch (city.toLowerCase()) {
            case "goa" -> 50000;
            case "bangalore" -> 40000;
            case "mumbai" -> 45000;
            default -> 20000;
        };
    }
}
