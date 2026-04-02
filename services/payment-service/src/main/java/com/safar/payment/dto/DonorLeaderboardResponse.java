package com.safar.payment.dto;

import java.util.List;

public record DonorLeaderboardResponse(
        List<LeaderboardEntry> topDonors,
        List<CityEntry> topCities,
        String period
) {
    public record LeaderboardEntry(
            String name,
            long totalPaise,
            int donationCount,
            String tier
    ) {}
    public record CityEntry(
            String city,
            long totalPaise,
            int donors
    ) {}
}
