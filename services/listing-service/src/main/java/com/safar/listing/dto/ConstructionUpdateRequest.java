package com.safar.listing.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record ConstructionUpdateRequest(
        @NotBlank String title,
        String description,
        Integer progressPercent,
        List<String> photos
) {}
