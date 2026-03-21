package com.safar.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SendOtpRequest(
        @NotBlank(message = "Phone number is required")
        @Pattern(
                regexp = "^\\+91[6-9]\\d{9}$",
                message = "Invalid Indian phone number. Format: +91XXXXXXXXXX"
        )
        String phone
) {}
