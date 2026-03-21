package com.safar.listing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterAppRequest(
        @NotBlank @Size(max = 100) String appName,
        String description,
        String redirectUris,
        String scopes,
        String webhookUrl
) {}
