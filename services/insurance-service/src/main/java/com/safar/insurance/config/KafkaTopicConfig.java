package com.safar.insurance.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic insurancePolicyIssuedTopic() {
        return new NewTopic("insurance.policy.issued", 1, (short) 1);
    }

    @Bean
    public NewTopic insurancePolicyCancelledTopic() {
        return new NewTopic("insurance.policy.cancelled", 1, (short) 1);
    }

    @Bean
    public NewTopic insurancePolicyAbandonedTopic() {
        return new NewTopic("insurance.policy.abandoned", 1, (short) 1);
    }
}
