package com.safar.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record PinSignInRequest(
        String phone,
        String email,
        @NotBlank String pin
) {}
