package com.safar.booking.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record RegisterCleanerRequest(
        @NotNull String fullName,
        @NotNull @Pattern(regexp = "^(\\+?91)?[6-9]\\d{9}$",
                message = "Phone must be a 10-digit Indian mobile number")
        String phone,
        @NotNull String cities,
        @NotNull Long ratePerHourPaise
) {}
