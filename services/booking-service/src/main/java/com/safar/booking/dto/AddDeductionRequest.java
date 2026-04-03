package com.safar.booking.dto;

public record AddDeductionRequest(
        String category,
        String description,
        long amountPaise,
        String evidenceUrl
) {}
