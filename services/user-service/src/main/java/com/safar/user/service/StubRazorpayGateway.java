package com.safar.user.service;

import com.safar.user.dto.RazorpaySubscription;
import com.safar.user.entity.enums.SubscriptionTier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Profile("test")
public class StubRazorpayGateway implements RazorpayGateway {

    @Override
    public RazorpaySubscription createSubscription(UUID hostId, SubscriptionTier tier) {
        return new RazorpaySubscription("rzp_sub_test_" + UUID.randomUUID(), "https://rzp.io/test");
    }

    @Override
    public void cancelSubscription(String razorpaySubId) {
        // no-op in tests
    }
}
