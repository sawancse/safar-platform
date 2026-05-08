package com.safar.services.dto;

import jakarta.validation.constraints.Pattern;

public record UpdateChefProfileRequest(
        String name,
        @Pattern(regexp = "^(\\+?91)?[6-9]\\d{9}$",
                message = "Phone must be a 10-digit Indian mobile number")
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
        Integer eventMaxPax,
        String profilePhotoUrl,
        String introVideoUrl
) {}
