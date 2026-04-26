package com.safar.services.dto;

public record CreateChefProfileRequest(
        String name,
        String phone,
        String email,
        String bio,
        String chefType,
        Integer experienceYears,
        String city,
        String state,
        String pincode,
        String cuisines,
        String specialties,
        String localities,
        Long dailyRatePaise,
        Long monthlyRatePaise,
        Long eventMinPlatePaise,
        String languages,
        Integer eventMinPax,
        Integer eventMaxPax
) {}
