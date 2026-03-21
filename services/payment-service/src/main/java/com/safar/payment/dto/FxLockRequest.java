package com.safar.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record FxLockRequest(
    @NotBlank String sourceCurrency,
    @NotNull @Positive Long sourceAmount,
    UUID bookingId
) {}
