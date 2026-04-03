package com.safar.search.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicConfig {

    // Listing events consumed
    @Bean public NewTopic listingVerified() { return new NewTopic("listing.verified", 1, (short) 1); }
    @Bean public NewTopic listingArchived() { return new NewTopic("listing.archived", 1, (short) 1); }
    @Bean public NewTopic listingSuspended() { return new NewTopic("listing.suspended", 1, (short) 1); }

    // Review events consumed
    @Bean public NewTopic reviewCreated() { return new NewTopic("review.created", 1, (short) 1); }

    // Sale property events consumed
    @Bean public NewTopic salePropertyIndexed() { return new NewTopic("sale.property.indexed", 1, (short) 1); }
    @Bean public NewTopic salePropertyDeleted() { return new NewTopic("sale.property.deleted", 1, (short) 1); }

    // Builder project events consumed
    @Bean public NewTopic builderProjectIndexed() { return new NewTopic("builder.project.indexed", 1, (short) 1); }

    // Experience events consumed
    @Bean public NewTopic experienceActivated() { return new NewTopic("experience.activated", 1, (short) 1); }
    @Bean public NewTopic experienceRejected() { return new NewTopic("experience.rejected", 1, (short) 1); }
}
