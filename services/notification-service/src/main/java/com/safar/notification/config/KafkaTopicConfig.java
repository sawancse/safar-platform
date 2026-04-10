package com.safar.notification.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicConfig {

    // Booking events consumed
    @Bean public NewTopic bookingCreated() { return new NewTopic("booking.created", 1, (short) 1); }
    @Bean public NewTopic bookingConfirmed() { return new NewTopic("booking.confirmed", 1, (short) 1); }
    @Bean public NewTopic bookingCancelled() { return new NewTopic("booking.cancelled", 1, (short) 1); }
    @Bean public NewTopic bookingCheckedIn() { return new NewTopic("booking.checked-in", 1, (short) 1); }
    @Bean public NewTopic bookingCompleted() { return new NewTopic("booking.completed", 1, (short) 1); }
    @Bean public NewTopic bookingExpired() { return new NewTopic("booking.expired", 1, (short) 1); }

    // Payment events consumed
    @Bean public NewTopic paymentCaptured() { return new NewTopic("payment.captured", 1, (short) 1); }
    @Bean public NewTopic paymentFailed() { return new NewTopic("payment.failed", 1, (short) 1); }
    @Bean public NewTopic paymentRefunded() { return new NewTopic("payment.refunded", 1, (short) 1); }
    @Bean public NewTopic paymentReminder() { return new NewTopic("payment.reminder", 1, (short) 1); }
    @Bean public NewTopic paymentReminderUrgent() { return new NewTopic("payment.reminder.urgent", 1, (short) 1); }

    // Review events consumed
    @Bean public NewTopic reviewCreated() { return new NewTopic("review.created", 1, (short) 1); }
    @Bean public NewTopic reviewReplied() { return new NewTopic("review.replied", 1, (short) 1); }

    // Chef events consumed
    @Bean public NewTopic chefBookingCreated() { return new NewTopic("chef.booking.created", 1, (short) 1); }
    @Bean public NewTopic chefBookingConfirmed() { return new NewTopic("chef.booking.confirmed", 1, (short) 1); }
    @Bean public NewTopic chefBookingCancelled() { return new NewTopic("chef.booking.cancelled", 1, (short) 1); }
    @Bean public NewTopic chefBookingCompleted() { return new NewTopic("chef.booking.completed", 1, (short) 1); }
    @Bean public NewTopic chefBookingPaymentConfirmed() { return new NewTopic("chef.booking.payment.confirmed", 1, (short) 1); }
    @Bean public NewTopic chefBookingModified() { return new NewTopic("chef.booking.modified", 1, (short) 1); }
    @Bean public NewTopic chefBookingReminder() { return new NewTopic("chef.booking.reminder", 1, (short) 1); }

    // Event booking events consumed
    @Bean public NewTopic eventBookingCreated() { return new NewTopic("event.booking.created", 1, (short) 1); }
    @Bean public NewTopic eventBookingQuoted() { return new NewTopic("event.booking.quoted", 1, (short) 1); }
    @Bean public NewTopic eventBookingConfirmed() { return new NewTopic("event.booking.confirmed", 1, (short) 1); }
    @Bean public NewTopic eventBookingAdvancePaid() { return new NewTopic("event.booking.advance.paid", 1, (short) 1); }
    @Bean public NewTopic eventBookingCompleted() { return new NewTopic("event.booking.completed", 1, (short) 1); }
    @Bean public NewTopic eventBookingCancelled() { return new NewTopic("event.booking.cancelled", 1, (short) 1); }

    // Medical & donation events consumed
    @Bean public NewTopic medicalBookingCreated() { return new NewTopic("medical.booking.created", 1, (short) 1); }
    @Bean public NewTopic donationCaptured() { return new NewTopic("donation.captured", 1, (short) 1); }
}
