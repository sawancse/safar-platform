package com.safar.payment.gateway;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component("stripeGateway")
@ConditionalOnProperty(name = "stripe.enabled", havingValue = "true", matchIfMissing = false)
public class StripePaymentGateway implements PaymentGateway {

    @Override
    public String name() {
        return "stripe";
    }

    @Override
    public OrderResult createOrder(long amountPaise, String currency, String receipt, String description) {
        throw new UnsupportedOperationException("Stripe integration pending — Phase 2");
    }

    @Override
    public CaptureResult capturePayment(String gatewayOrderId, String gatewayPaymentId, String signature) {
        throw new UnsupportedOperationException("Stripe integration pending — Phase 2");
    }

    @Override
    public RefundResult refund(String gatewayPaymentId, long amountPaise, String reason) {
        throw new UnsupportedOperationException("Stripe integration pending — Phase 2");
    }

    @Override
    public boolean verifyWebhook(String payload, String signature, String secret) {
        throw new UnsupportedOperationException("Stripe integration pending — Phase 2");
    }

    @Override
    public SubscriptionResult createSubscription(String planName, long amountPaise, String currency) {
        throw new UnsupportedOperationException("Stripe integration pending — Phase 2");
    }
}
