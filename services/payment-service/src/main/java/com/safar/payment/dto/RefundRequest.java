package com.safar.payment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record RefundRequest(
    @NotNull UUID paymentId,
    UUID bookingId,
    @NotNull @Positive Long amountPaise,
    @NotNull String reason,
    @NotNull String refundType
) {}
