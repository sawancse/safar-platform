package com.safar.chef.dto;

import java.time.LocalDate;
import java.util.UUID;

public record CreateEventBookingRequest(
        UUID chefId,
        String eventType,
        LocalDate eventDate,
        String eventTime,
        Integer durationHours,
        Integer guestCount,
        String venueAddress,
        String city,
        String locality,
        String pincode,
        UUID menuPackageId,
        String menuDescription,
        String cuisinePreferences,
        Boolean decorationRequired,
        Boolean cakeRequired,
        Boolean staffRequired,
        Integer staffCount,
        String specialRequests,
        String customerName,
        String customerPhone,
        String customerEmail,
        String servicesJson
) {}
