package com.safar.payment.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record InitiatePayoutRequest(
        @NotNull UUID bookingId,
        @NotNull Long amountPaise,
        @NotNull String upiId
) {}
