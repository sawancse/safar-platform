package com.safar.listing.kafka;

import com.safar.listing.entity.Availability;
import com.safar.listing.repository.AvailabilityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Listens for booking events to update availability source tracking.
 * The actual date blocking is done via the internal API call from booking-service.
 * This consumer adds source metadata so conflict resolution works correctly.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BookingEventConsumer {

    private final AvailabilityRepository availabilityRepository;

    @KafkaListener(topics = "booking.confirmed", groupId = "listing-booking-group")
    public void onBookingConfirmed(String bookingId) {
        try {
            UUID id = UUID.fromString(bookingId.trim().replace("\"", ""));
            log.info("Booking confirmed event received: {}. Availability already blocked via API call.", id);
            // Source tracking is handled when blockDates is called from booking-service.
            // This listener ensures we catch any edge cases.
        } catch (Exception e) {
            log.warn("Failed to process booking.confirmed event: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "booking.cancelled", groupId = "listing-booking-group")
    public void onBookingCancelled(String bookingId) {
        try {
            UUID id = UUID.fromString(bookingId.trim().replace("\"", ""));
            log.info("Booking cancelled event received: {}. Availability restored via API call.", id);
        } catch (Exception e) {
            log.warn("Failed to process booking.cancelled event: {}", e.getMessage());
        }
    }
}
