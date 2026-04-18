package com.safar.payment.gateway;

public record SubscriptionResult(String subscriptionId, String planId, String status, String shortUrl) {
    /** Legacy ctor for gateways that don't supply a short URL (e.g. Stripe stub). */
    public SubscriptionResult(String subscriptionId, String planId, String status) {
        this(subscriptionId, planId, status, null);
    }
}
