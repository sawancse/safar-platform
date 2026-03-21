package com.safar.listing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RoomTypeInclusionRequest(
        @NotNull String category,      // MEAL, DISCOUNT, FLEXIBILITY, WELLNESS, TRANSPORT, AMENITY, EXPERIENCE
        @NotBlank String name,          // "Breakfast", "10% Spa Discount", "Early Check-in"
        String description,             // "Continental buffet breakfast served 7-10am"
        String inclusionMode,           // INCLUDED, PAID_ADDON, COMPLIMENTARY (default: INCLUDED)
        Long chargePaise,               // Price if PAID_ADDON (default: 0)
        String chargeType,              // PER_NIGHT, PER_STAY, PER_PERSON, PER_HOUR, PER_USE
        Integer discountPercent,        // For DISCOUNT category (e.g., 10 = 10% off)
        String terms,                   // "Subject to availability"
        Boolean isHighlight,            // Show on listing card
        Integer sortOrder               // Display order
) {}
