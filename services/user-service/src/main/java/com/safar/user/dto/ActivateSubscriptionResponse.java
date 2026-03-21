package com.safar.user.dto;

import com.safar.user.entity.enums.SubscriptionStatus;
import com.safar.user.entity.enums.SubscriptionTier;

public record ActivateSubscriptionResponse(
        String razorpaySubId,
        String paymentLink,
        SubscriptionTier tier,
        SubscriptionStatus status
) {}
