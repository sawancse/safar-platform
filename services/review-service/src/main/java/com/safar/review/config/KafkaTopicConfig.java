package com.safar.review.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic reviewCreatedTopic() {
        return new NewTopic("review.created", 1, (short) 1);
    }

    @Bean
    public NewTopic reviewRepliedTopic() {
        return new NewTopic("review.replied", 1, (short) 1);
    }
}
