package com.safar.listing.dto;

public record AdminReviewRequest(
        boolean approve,
        String adminNote
) {}
