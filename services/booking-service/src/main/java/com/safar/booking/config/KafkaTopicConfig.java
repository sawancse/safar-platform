package com.safar.booking.config;

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
    public NewTopic bookingCreatedTopic() {
        return new NewTopic("booking.created", 1, (short) 1);
    }

    @Bean
    public NewTopic bookingConfirmedTopic() {
        return new NewTopic("booking.confirmed", 1, (short) 1);
    }

    @Bean
    public NewTopic bookingCancelledTopic() {
        return new NewTopic("booking.cancelled", 1, (short) 1);
    }

    @Bean
    public NewTopic bookingCheckedInTopic() {
        return new NewTopic("booking.checked-in", 1, (short) 1);
    }

    @Bean
    public NewTopic bookingCompletedTopic() {
        return new NewTopic("booking.completed", 1, (short) 1);
    }

    @Bean
    public NewTopic bookingExpiredTopic() {
        return new NewTopic("booking.expired", 1, (short) 1);
    }

    @Bean
    public NewTopic bookingRecurringCreatedTopic() {
        return new NewTopic("booking.recurring.created", 1, (short) 1);
    }

    @Bean
    public NewTopic bookingRecurringCancelledTopic() {
        return new NewTopic("booking.recurring.cancelled", 1, (short) 1);
    }

    @Bean
    public NewTopic medicalBookingCreatedTopic() {
        return new NewTopic("medical.booking.created", 1, (short) 1);
    }

    @Bean
    public NewTopic tenancyCreatedTopic() {
        return new NewTopic("tenancy.created", 1, (short) 1);
    }

    @Bean
    public NewTopic tenancyNoticeTopic() {
        return new NewTopic("tenancy.notice", 1, (short) 1);
    }

    @Bean
    public NewTopic tenancyVacatedTopic() {
        return new NewTopic("tenancy.vacated", 1, (short) 1);
    }

    @Bean
    public NewTopic tenancyInvoiceGeneratedTopic() {
        return new NewTopic("tenancy.invoice.generated", 1, (short) 1);
    }

    @Bean
    public NewTopic liveAnywhereSubscribedTopic() {
        return new NewTopic("live_anywhere.subscribed", 1, (short) 1);
    }

    @Bean
    public NewTopic milesEarnedTopic() {
        return new NewTopic("miles.earned", 1, (short) 1);
    }

    @Bean
    public NewTopic milesRedeemedTopic() {
        return new NewTopic("miles.redeemed", 1, (short) 1);
    }

    @Bean
    public NewTopic cleaningJobCompletedTopic() {
        return new NewTopic("cleaning.job.completed", 1, (short) 1);
    }
}
