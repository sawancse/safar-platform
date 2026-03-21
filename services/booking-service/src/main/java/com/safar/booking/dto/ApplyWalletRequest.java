package com.safar.booking.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ApplyWalletRequest(
        @NotNull @Min(1) Long creditsToApplyPaise
) {}
