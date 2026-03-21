package com.safar.listing.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record MedicalPackageRequest(
        @NotNull UUID hospitalId,
        boolean includesPickup,
        boolean includesTranslator,
        boolean caregiverFriendly,
        @Positive long medicalPricePaise,
        @Positive int minStayNights
) {}
