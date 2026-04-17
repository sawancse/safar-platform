package com.safar.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VerifyEmailOtpRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email address")
        String email,

        @NotBlank(message = "OTP is required")
        @Size(min = 6, max = 6, message = "OTP must be 6 digits")
        String otp,

        @Size(min = 2, max = 100, message = "Name must be 2-100 characters")
        String name,

        // For new-user signup: the phone collected alongside email so a single person
        // doesn't end up with two separate auth accounts (phone-only and email-only).
        @Pattern(regexp = "^$|^\\+91[6-9]\\d{9}$", message = "Invalid Indian phone number")
        String phone
) {}
