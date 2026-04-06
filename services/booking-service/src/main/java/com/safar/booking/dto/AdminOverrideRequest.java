package com.safar.booking.dto;

public record AdminOverrideRequest(
        String notes,
        Long overrideRefundPaise
) {}
