package com.safar.user.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic loyaltyTierUpgradedTopic() {
        return new NewTopic("loyalty.tier.upgraded", 1, (short) 1);
    }

    @Bean
    public NewTopic referralSignupTopic() {
        return new NewTopic("referral.signup", 1, (short) 1);
    }

    @Bean
    public NewTopic referralCompletedTopic() {
        return new NewTopic("referral.completed", 1, (short) 1);
    }

    // Consumed
    @Bean
    public NewTopic hostRegisteredTopic() {
        return new NewTopic("host.registered", 1, (short) 1);
    }

    // Produced
    @Bean
    public NewTopic nomadPrimeSubscribedTopic() {
        return new NewTopic("nomad_prime.subscribed", 1, (short) 1);
    }

    @Bean
    public NewTopic conciergeDailyTopic() {
        return new NewTopic("concierge.daily", 1, (short) 1);
    }
}
