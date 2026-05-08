package com.safar.services.dto;

import jakarta.validation.constraints.Pattern;

public record StaffMemberRequest(
        String name,
        String role,
        @Pattern(regexp = "^(\\+?91)?[6-9]\\d{9}$",
                message = "Phone must be a 10-digit Indian mobile number")
        String phone,
        String photoUrl,
        Long hourlyRatePaise,
        String languages,
        Integer yearsExperience,
        String notes,
        Boolean active
) {}
