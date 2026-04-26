package com.safar.services.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic chefRegisteredTopic() {
        return new NewTopic("chef.registered", 1, (short) 1);
    }

    @Bean
    public NewTopic chefBookingCreatedTopic() {
        return new NewTopic("chef.booking.created", 1, (short) 1);
    }

    @Bean
    public NewTopic chefBookingConfirmedTopic() {
        return new NewTopic("chef.booking.confirmed", 1, (short) 1);
    }

    @Bean
    public NewTopic chefBookingCancelledTopic() {
        return new NewTopic("chef.booking.cancelled", 1, (short) 1);
    }

    @Bean
    public NewTopic chefBookingCompletedTopic() {
        return new NewTopic("chef.booking.completed", 1, (short) 1);
    }

    @Bean
    public NewTopic chefBookingPaymentConfirmedTopic() {
        return new NewTopic("chef.booking.payment.confirmed", 1, (short) 1);
    }

    @Bean
    public NewTopic chefBookingModifiedTopic() {
        return new NewTopic("chef.booking.modified", 1, (short) 1);
    }

    @Bean
    public NewTopic chefBookingReminderTopic() {
        return new NewTopic("chef.booking.reminder", 1, (short) 1);
    }

    @Bean
    public NewTopic chefBookingRefundRequestedTopic() {
        return new NewTopic("chef.booking.refund.requested", 1, (short) 1);
    }

    // Event booking topics
    @Bean
    public NewTopic eventBookingCreatedTopic() {
        return new NewTopic("event.booking.created", 1, (short) 1);
    }

    @Bean
    public NewTopic eventBookingQuotedTopic() {
        return new NewTopic("event.booking.quoted", 1, (short) 1);
    }

    @Bean
    public NewTopic eventBookingConfirmedTopic() {
        return new NewTopic("event.booking.confirmed", 1, (short) 1);
    }

    @Bean
    public NewTopic eventBookingAdvancePaidTopic() {
        return new NewTopic("event.booking.advance.paid", 1, (short) 1);
    }

    @Bean
    public NewTopic eventBookingCompletedTopic() {
        return new NewTopic("event.booking.completed", 1, (short) 1);
    }

    @Bean
    public NewTopic eventBookingCancelledTopic() {
        return new NewTopic("event.booking.cancelled", 1, (short) 1);
    }
}
