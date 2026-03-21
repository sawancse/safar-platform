package com.safar.user.dto;

import jakarta.validation.constraints.NotNull;

public record CohostProfileRequest(
        String bio,
        @NotNull String servicesOffered,
        @NotNull String cities,
        Integer minFeePct,
        Integer maxFeePct,
        Integer maxListings
) {}
