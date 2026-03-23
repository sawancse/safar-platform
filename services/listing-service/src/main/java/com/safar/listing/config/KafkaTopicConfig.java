package com.safar.listing.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic hostRegisteredTopic() {
        return new NewTopic("host.registered", 1, (short) 1);
    }

    @Bean
    public NewTopic listingVerifiedTopic() {
        return new NewTopic("listing.verified", 1, (short) 1);
    }

    @Bean
    public NewTopic listingArchivedTopic() {
        return new NewTopic("listing.archived", 1, (short) 1);
    }

    @Bean
    public NewTopic listingSuspendedTopic() {
        return new NewTopic("listing.suspended", 1, (short) 1);
    }

    @Bean
    public NewTopic listingRwCertifiedTopic() {
        return new NewTopic("listing.rw_certified", 1, (short) 1);
    }

    @Bean
    public NewTopic aashrayCaseCreatedTopic() {
        return new NewTopic("aashray.case.created", 1, (short) 1);
    }

    @Bean
    public NewTopic aashrayCaseHousedTopic() {
        return new NewTopic("aashray.case.housed", 1, (short) 1);
    }

    @Bean
    public NewTopic aashrayCaseMatchedTopic() {
        return new NewTopic("aashray.case.matched", 1, (short) 1);
    }

    @Bean
    public NewTopic vpnListingEnrolledTopic() {
        return new NewTopic("vpn.listing.enrolled", 1, (short) 1);
    }

    @Bean
    public NewTopic seekerProfileCreatedTopic() {
        return new NewTopic("seeker.profile.created", 1, (short) 1);
    }

    @Bean
    public NewTopic managedListingEnrolledTopic() {
        return new NewTopic("managed.listing.enrolled", 1, (short) 1);
    }

    @Bean
    public NewTopic guaranteeContractCreatedTopic() {
        return new NewTopic("guarantee.contract.created", 1, (short) 1);
    }

    @Bean
    public NewTopic experienceBookedTopic() {
        return new NewTopic("experience.booked", 1, (short) 1);
    }

    @Bean
    public NewTopic notificationHostDemandAlertTopic() {
        return new NewTopic("notification.host.demand_alert", 1, (short) 1);
    }

    @Bean
    public NewTopic channelBookingReceivedTopic() {
        return new NewTopic("channel.booking.received", 1, (short) 1);
    }

    @Bean
    public NewTopic channelBookingCancelledTopic() {
        return new NewTopic("channel.booking.cancelled", 1, (short) 1);
    }
}
