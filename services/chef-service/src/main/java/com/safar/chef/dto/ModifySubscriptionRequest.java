package com.safar.chef.dto;

public record ModifySubscriptionRequest(
    Integer mealsPerDay,
    String mealTypes,
    String schedule,
    String address,
    String city,
    String locality,
    String pincode,
    String specialRequests,
    String dietaryPreferences
) {}
