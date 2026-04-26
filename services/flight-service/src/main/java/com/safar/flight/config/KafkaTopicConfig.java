package com.safar.flight.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic flightBookingCreatedTopic() {
        return new NewTopic("flight.booking.created", 1, (short) 1);
    }

    @Bean
    public NewTopic flightBookingConfirmedTopic() {
        return new NewTopic("flight.booking.confirmed", 1, (short) 1);
    }

    @Bean
    public NewTopic flightBookingCancelledTopic() {
        return new NewTopic("flight.booking.cancelled", 1, (short) 1);
    }

    @Bean
    public NewTopic flightBookingExpiredTopic() {
        return new NewTopic("flight.booking.expired", 1, (short) 1);
    }

    @Bean
    public NewTopic flightReminderCheckinTopic() {
        return new NewTopic("flight.reminder.checkin", 1, (short) 1);
    }

    @Bean
    public NewTopic flightAirlineChangedTopic() {
        return new NewTopic("flight.airline.changed", 1, (short) 1);
    }

    @Bean
    public NewTopic flightSearchAbandonedTopic() {
        return new NewTopic("flight.search.abandoned", 1, (short) 1);
    }
}
