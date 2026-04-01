package com.safar.payment.dto;

import java.time.OffsetDateTime;

public record DonationResponse(
        String id,
        String donationRef,
        Long amountPaise,
        String currency,
        String frequency,
        String status,
        String donorName,
        String donorEmail,
        String dedicatedTo,
        String dedicationMessage,
        String receiptNumber,
        String campaignCode,
        OffsetDateTime capturedAt,
        OffsetDateTime createdAt
) {}
