package com.safar.user.service;

import com.safar.user.dto.RazorpaySubscription;
import com.safar.user.entity.enums.SubscriptionTier;

import java.util.UUID;

public interface RazorpayGateway {
    RazorpaySubscription createSubscription(UUID hostId, SubscriptionTier tier);
    void cancelSubscription(String razorpaySubId);
}
