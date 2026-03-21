package com.safar.listing.dto;

import java.util.UUID;

public record DemandAlertDto(
        UUID listingId,
        String alertType,
        String messageEn,
        Long suggestedPricePaise,
        double demandMultiplier,
        String urgency
) {}
