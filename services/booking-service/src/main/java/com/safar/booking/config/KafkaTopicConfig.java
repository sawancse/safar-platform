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
}
