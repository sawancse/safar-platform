package com.safar.user.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OrganizationDto(
        UUID id,
        String name,
        String type,
        String unhcrPartnerCode,
        String contactEmail,
        String contactPhone,
        Long budgetPaise,
        Long spentPaise,
        Long availableBudgetPaise,
        Boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
