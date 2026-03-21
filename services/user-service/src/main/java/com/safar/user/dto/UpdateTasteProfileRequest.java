package com.safar.user.dto;

import com.safar.user.entity.enums.BudgetTier;
import com.safar.user.entity.enums.GroupType;
import com.safar.user.entity.enums.PropertyVibe;
import com.safar.user.entity.enums.TravelStyle;

import java.util.List;

public record UpdateTasteProfileRequest(
        TravelStyle travelStyle,
        PropertyVibe propertyVibe,
        List<String> mustHaves,
        GroupType groupType,
        BudgetTier budgetTier
) {}
