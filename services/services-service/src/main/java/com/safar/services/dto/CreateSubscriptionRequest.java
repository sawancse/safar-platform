package com.safar.services.dto;

import java.time.LocalDate;
import java.util.UUID;

public record CreateSubscriptionRequest(
        UUID chefId,
        String plan,
        Integer mealsPerDay,
        String mealTypes,
        String schedule,
        Long monthlyRatePaise,
        LocalDate startDate,
        String address,
        String city,
        String locality,
        String pincode,
        String specialRequests,
        String dietaryPreferences,
        String customerName
) {}
