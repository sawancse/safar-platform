package com.safar.user.dto;

import com.safar.user.entity.enums.SubscriptionStatus;
import com.safar.user.entity.enums.SubscriptionTier;

import java.time.OffsetDateTime;
import java.util.UUID;

public record HostSubscriptionDto(
        UUID id,
        UUID hostId,
        SubscriptionTier tier,
        SubscriptionStatus status,
        OffsetDateTime trialEndsAt,
        String billingCycle,
        Integer amountPaise,
        OffsetDateTime nextBillingAt,
        OffsetDateTime createdAt,
        Integer commissionDiscountPercent,
        Boolean preferredPartner
) {}
