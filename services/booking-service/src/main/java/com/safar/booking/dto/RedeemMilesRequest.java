package com.safar.booking.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RedeemMilesRequest(
        @NotNull UUID bookingId,
        @NotNull @Min(1) Long miles
) {}
