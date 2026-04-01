package com.safar.payment.dto;

import java.util.List;

public record DonationStatsResponse(
        long totalRaisedPaise,
        long goalPaise,
        int totalDonors,
        int familiesHoused,
        int monthlyDonors,
        int progressPercent,
        String activeCampaign,
        String campaignTagline,
        List<RecentDonor> recentDonors
) {
    public record RecentDonor(
            String name,
            long amountPaise,
            String city,
            long minutesAgo
    ) {}
}
