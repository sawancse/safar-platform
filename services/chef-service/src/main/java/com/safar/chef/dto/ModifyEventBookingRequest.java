package com.safar.chef.dto;

import java.time.LocalDate;

public record ModifyEventBookingRequest(
    LocalDate eventDate,
    String eventTime,
    Integer durationHours,
    Integer guestCount,
    String venueAddress,
    String city,
    String locality,
    String pincode,
    String menuDescription,
    String cuisinePreferences,
    Boolean decorationRequired,
    Boolean cakeRequired,
    Boolean staffRequired,
    Integer staffCount,
    String specialRequests,
    String servicesJson
) {}
