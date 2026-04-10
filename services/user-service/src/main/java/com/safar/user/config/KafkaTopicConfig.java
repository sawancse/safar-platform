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

    // Lead management topics
    @Bean public NewTopic leadCapturedTopic() { return new NewTopic("lead.captured", 1, (short) 1); }
    @Bean public NewTopic leadConvertedTopic() { return new NewTopic("lead.converted", 1, (short) 1); }
    @Bean public NewTopic leadNurtureWelcomeTopic() { return new NewTopic("lead.nurture.welcome", 1, (short) 1); }
    @Bean public NewTopic leadNurtureDay3Topic() { return new NewTopic("lead.nurture.day3", 1, (short) 1); }
    @Bean public NewTopic leadNurtureDay7Topic() { return new NewTopic("lead.nurture.day7", 1, (short) 1); }
    @Bean public NewTopic leadNurtureReEngagementTopic() { return new NewTopic("lead.nurture.re-engagement", 1, (short) 1); }
    @Bean public NewTopic leadPriceDropTopic() { return new NewTopic("lead.price.dropped", 1, (short) 1); }
    @Bean public NewTopic leadLocalityNewTopic() { return new NewTopic("lead.locality.new-listing", 1, (short) 1); }
}
