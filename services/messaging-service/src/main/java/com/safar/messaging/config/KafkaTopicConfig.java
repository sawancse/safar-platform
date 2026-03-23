package com.safar.messaging.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic messageCreatedTopic() {
        return new NewTopic("message.created", 1, (short) 1);
    }
}
