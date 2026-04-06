package com.safar.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SetPinRequest(
        @NotBlank @Pattern(regexp = "^\\d{4,6}$", message = "PIN must be 4-6 digits")
        String pin
) {}
