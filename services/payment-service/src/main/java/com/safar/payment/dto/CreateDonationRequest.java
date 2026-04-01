package com.safar.payment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateDonationRequest(
        @NotNull @Min(10000) Long amountPaise,    // min ₹100
        String frequency,                          // ONE_TIME or MONTHLY
        String donorName,
        String donorEmail,
        String donorPhone,
        String donorPan,
        String dedicatedTo,
        String dedicationMessage,
        String campaignCode
) {}
