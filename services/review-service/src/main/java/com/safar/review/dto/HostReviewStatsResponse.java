package com.safar.review.dto;

public record HostReviewStatsResponse(
        long totalReviews,
        long pendingReplies,
        long repliedReviews
) {}
