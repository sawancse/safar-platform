package com.safar.listing.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PgPackageRequest(
        @NotBlank String name,
        String description,
        @NotNull @Min(0) Long monthlyPricePaise,
        Boolean includesMeals,
        Boolean includesLaundry,
        Boolean includesWifi,
        Boolean includesHousekeeping
) {}
