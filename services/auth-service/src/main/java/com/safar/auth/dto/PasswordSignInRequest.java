package com.safar.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PasswordSignInRequest(
        @NotBlank @Email String email,
        @NotBlank String password
) {}
