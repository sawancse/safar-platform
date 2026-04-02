package com.safar.payment.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic paymentCapturedTopic() {
        return new NewTopic("payment.captured", 1, (short) 1);
    }

    @Bean
    public NewTopic paymentRefundedTopic() {
        return new NewTopic("payment.refunded", 1, (short) 1);
    }

    @Bean
    public NewTopic paymentFailedTopic() {
        return new NewTopic("payment.failed", 1, (short) 1);
    }

    // --- PG Tenancy Subscription topics ---

    @Bean
    public NewTopic tenancySubscriptionCreatedTopic() {
        return new NewTopic("tenancy.subscription.created", 1, (short) 1);
    }

    @Bean
    public NewTopic tenancySubscriptionAuthenticatedTopic() {
        return new NewTopic("tenancy.subscription.authenticated", 1, (short) 1);
    }

    @Bean
    public NewTopic tenancySubscriptionChargedTopic() {
        return new NewTopic("tenancy.subscription.charged", 1, (short) 1);
    }

    @Bean
    public NewTopic tenancySubscriptionHaltedTopic() {
        return new NewTopic("tenancy.subscription.halted", 1, (short) 1);
    }

    // --- Host Payout topics ---

    @Bean
    public NewTopic tenancyRentCollectedTopic() {
        return new NewTopic("tenancy.rent.collected", 1, (short) 1);
    }

    @Bean
    public NewTopic hostPayoutCompletedTopic() {
        return new NewTopic("host.payout.completed", 1, (short) 1);
    }

    @Bean
    public NewTopic hostPayoutFailedTopic() {
        return new NewTopic("host.payout.failed", 1, (short) 1);
    }

    // --- RazorpayX Payout webhook event topics ---

    @Bean
    public NewTopic payoutCompletedTopic() {
        return new NewTopic("payout.completed", 1, (short) 1);
    }

    @Bean
    public NewTopic payoutReversedTopic() {
        return new NewTopic("payout.reversed", 1, (short) 1);
    }

    @Bean
    public NewTopic payoutFailedTopic() {
        return new NewTopic("payout.failed", 1, (short) 1);
    }

    // --- Donation topics ---

    @Bean
    public NewTopic donationCapturedTopic() {
        return new NewTopic("donation.captured", 1, (short) 1);
    }
}
