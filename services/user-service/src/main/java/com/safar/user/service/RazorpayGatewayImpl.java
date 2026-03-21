package com.safar.user.service;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Subscription;
import com.safar.user.dto.RazorpaySubscription;
import com.safar.user.entity.enums.SubscriptionTier;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Profile("!test")
@Slf4j
public class RazorpayGatewayImpl implements RazorpayGateway {

    @Value("${razorpay.key-id}")
    private String keyId;

    @Value("${razorpay.key-secret}")
    private String keySecret;

    @Value("${razorpay.plans.starter}")
    private String starterPlanId;

    @Value("${razorpay.plans.pro}")
    private String proPlanId;

    @Value("${razorpay.plans.commercial}")
    private String commercialPlanId;

    @Override
    public RazorpaySubscription createSubscription(UUID hostId, SubscriptionTier tier) {
        try {
            RazorpayClient client = new RazorpayClient(keyId, keySecret);
            JSONObject request = new JSONObject();
            request.put("plan_id", getPlanId(tier));
            request.put("total_count", 120);
            request.put("quantity", 1);
            request.put("customer_notify", 1);

            Subscription sub = client.subscriptions.create(request);
            log.info("Created Razorpay subscription {} for host {}", sub.get("id"), hostId);
            return new RazorpaySubscription(sub.get("id"), sub.get("short_url"));
        } catch (RazorpayException e) {
            throw new RuntimeException("Razorpay error: " + e.getMessage(), e);
        }
    }

    @Override
    public void cancelSubscription(String razorpaySubId) {
        try {
            RazorpayClient client = new RazorpayClient(keyId, keySecret);
            JSONObject cancelReq = new JSONObject();
            cancelReq.put("cancel_at_cycle_end", 0);
            client.subscriptions.cancel(razorpaySubId, cancelReq);
        } catch (RazorpayException e) {
            throw new RuntimeException("Razorpay cancel error: " + e.getMessage(), e);
        }
    }

    private String getPlanId(SubscriptionTier tier) {
        return switch (tier) {
            case STARTER -> starterPlanId;
            case PRO -> proPlanId;
            case COMMERCIAL -> commercialPlanId;
        };
    }
}
