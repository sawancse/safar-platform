package com.safar.services.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateEventPricingRequest(
        @NotBlank String itemKey,
        @NotNull Long pricePaise,
        Boolean available
) {}
