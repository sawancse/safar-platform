package com.safar.user.dto;

import com.safar.user.entity.enums.BudgetTier;
import com.safar.user.entity.enums.GroupType;
import com.safar.user.entity.enums.PropertyVibe;
import com.safar.user.entity.enums.TravelStyle;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record TasteProfileDto(
        UUID id,
        UUID userId,
        TravelStyle travelStyle,
        PropertyVibe propertyVibe,
        List<String> mustHaves,
        GroupType groupType,
        BudgetTier budgetTier,
        OffsetDateTime updatedAt
) {}
