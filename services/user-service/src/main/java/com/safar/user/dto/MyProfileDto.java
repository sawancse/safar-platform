package com.safar.user.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MyProfileDto(
        UUID userId,
        String name,
        String displayName,
        String email,
        String avatarUrl,
        String phone,
        String role,
        String language,
        LocalDate dateOfBirth,
        String gender,
        String nationality,
        String address,
        String passportName,
        String passportNumber,
        LocalDate passportExpiry,
        String bio,
        String languages,
        Integer responseRate,
        Integer avgResponseMinutes,
        Integer totalHostReviews,
        Integer profileCompletion,
        OffsetDateTime updatedAt
) {}
