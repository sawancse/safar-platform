package com.safar.payment.gateway;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PaymentGatewayRouter {

    private final List<PaymentGateway> gateways;

    public PaymentGateway route(String currency) {
        if ("INR".equals(currency)) {
            return findGateway("razorpay");
        }
        // Phase 2: route USD/EUR/GBP to Stripe
        return findGateway("razorpay"); // fallback to Razorpay for now
    }

    public PaymentGateway findGateway(String name) {
        return gateways.stream()
                .filter(g -> g.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No gateway found: " + name));
    }
}
