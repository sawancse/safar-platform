package com.safar.chef.dto;

public record StaffMemberRequest(
        String name,
        String role,
        String phone,
        String photoUrl,
        Long hourlyRatePaise,
        String languages,
        Integer yearsExperience,
        String notes,
        Boolean active
) {}
