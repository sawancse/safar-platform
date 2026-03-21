package com.safar.booking.dto;

import jakarta.validation.constraints.NotNull;

public record RegisterCleanerRequest(
        @NotNull String fullName,
        @NotNull String phone,
        @NotNull String cities,
        @NotNull Long ratePerHourPaise
) {}
