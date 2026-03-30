package com.safar.chef.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicConfig {

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
}
