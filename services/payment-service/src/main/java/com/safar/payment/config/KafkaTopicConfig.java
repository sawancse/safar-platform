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
}
