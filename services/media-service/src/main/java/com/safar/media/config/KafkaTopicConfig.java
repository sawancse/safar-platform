package com.safar.media.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic mediaUploadedTopic() {
        return new NewTopic("media.uploaded", 1, (short) 1);
    }
}
