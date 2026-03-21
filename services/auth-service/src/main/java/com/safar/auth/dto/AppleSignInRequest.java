package com.safar.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record AppleSignInRequest(
        @NotBlank String identityToken,
        String name
) {}
