package com.safar.listing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreateGroupRequest(
        @NotBlank String name,
        Integer bundleDiscountPct,
        @NotNull List<UUID> listingIds
) {}
