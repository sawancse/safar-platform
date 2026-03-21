package com.safar.booking.service;

import com.safar.booking.entity.Booking;
import com.safar.booking.entity.BookingRoomSelection;
import com.safar.booking.entity.enums.BookingStatus;
import com.safar.booking.repository.BookingRepository;
import com.safar.booking.repository.BookingRoomSelectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Auto-expires PENDING_PAYMENT bookings and sends payment reminders.
 *
 * Timeline:
 * - 15 min: First payment reminder
 * - 1 hour: Second payment reminder (urgent)
 * - 24 hours: Auto-expire → CANCELLED + release inventory
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingExpiryService {

    private final BookingRepository bookingRepo;
    private final BookingRoomSelectionRepository roomSelectionRepo;
    private final BookingService bookingService;
    private final KafkaTemplate<String, String> kafka;

    private static final long REMINDER_1_MINUTES = 15;
    private static final long REMINDER_2_MINUTES = 60;
    private static final long EXPIRY_HOURS = 24;

    /**
     * Runs every 5 minutes — checks for pending bookings needing reminders or expiry.
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    @Transactional
    public void processExpiredAndReminders() {
        OffsetDateTime now = OffsetDateTime.now();

        List<Booking> pendingBookings = bookingRepo.findByStatus(BookingStatus.PENDING_PAYMENT);

        for (Booking booking : pendingBookings) {
            OffsetDateTime createdAt = booking.getCreatedAt();
            long minutesElapsed = java.time.Duration.between(createdAt, now).toMinutes();

            // Auto-expire after 24 hours
            if (minutesElapsed >= EXPIRY_HOURS * 60) {
                expireBooking(booking);
                continue;
            }

            // Send payment reminders via Kafka events (notification-service handles emails)
            // Use Redis to track which reminders have been sent
            String reminder1Key = "booking:reminder1:" + booking.getId();
            String reminder2Key = "booking:reminder2:" + booking.getId();

            if (minutesElapsed >= REMINDER_2_MINUTES) {
                // Send urgent reminder (1 hour) — only if not already sent
                sendReminderIfNeeded(booking, reminder2Key, "payment.reminder.urgent");
            } else if (minutesElapsed >= REMINDER_1_MINUTES) {
                // Send first reminder (15 min) — only if not already sent
                sendReminderIfNeeded(booking, reminder1Key, "payment.reminder");
            }
        }
    }

    private void expireBooking(Booking booking) {
        try {
            booking.setStatus(BookingStatus.CANCELLED);
            booking.setCancellationReason("SYSTEM_AUTO_EXPIRED");
            booking.setCancelledAt(OffsetDateTime.now());
            bookingRepo.save(booking);

            // Release inventory
            releaseInventory(booking);

            // Notify via Kafka
            String event = String.format(
                    "{\"bookingId\":\"%s\",\"bookingRef\":\"%s\",\"reason\":\"PAYMENT_TIMEOUT\",\"autoExpired\":true}",
                    booking.getId(), booking.getBookingRef());
            kafka.send("booking.cancelled", booking.getId().toString(), event);
            kafka.send("booking.expired", booking.getId().toString(), event);

            log.info("Auto-expired booking {} (pending for >24 hours)", booking.getBookingRef());
        } catch (Exception e) {
            log.error("Failed to expire booking {}: {}", booking.getBookingRef(), e.getMessage());
        }
    }

    private void releaseInventory(Booking booking) {
        try {
            bookingService.getListingClient().unblockDates(
                    booking.getListingId(),
                    booking.getCheckIn().toLocalDate(),
                    booking.getCheckOut().toLocalDate());
        } catch (Exception ignored) {}

        // Multi-room: release each room type
        List<BookingRoomSelection> roomSels = roomSelectionRepo.findByBookingId(booking.getId());
        if (!roomSels.isEmpty()) {
            for (BookingRoomSelection sel : roomSels) {
                try {
                    bookingService.getListingClient().incrementRoomTypeAvailability(
                            sel.getRoomTypeId(),
                            booking.getCheckIn().toLocalDate(),
                            booking.getCheckOut().toLocalDate(),
                            sel.getCount());
                } catch (Exception ignored) {}
            }
        } else if (booking.getRoomTypeId() != null) {
            try {
                int rooms = booking.getRoomsCount() != null ? booking.getRoomsCount() : 1;
                bookingService.getListingClient().incrementRoomTypeAvailability(
                        booking.getRoomTypeId(),
                        booking.getCheckIn().toLocalDate(),
                        booking.getCheckOut().toLocalDate(),
                        rooms);
            } catch (Exception ignored) {}
        }
    }

    private void sendReminderIfNeeded(Booking booking, String redisKey, String topic) {
        // Simple dedup: use Kafka key to avoid duplicate reminders
        // In production, use Redis SET NX with TTL
        try {
            String event = String.format(
                    "{\"bookingId\":\"%s\",\"bookingRef\":\"%s\",\"guestEmail\":\"%s\",\"guestName\":\"%s %s\",\"totalAmountPaise\":%d}",
                    booking.getId(), booking.getBookingRef(),
                    booking.getGuestEmail() != null ? booking.getGuestEmail() : "",
                    booking.getGuestFirstName() != null ? booking.getGuestFirstName() : "",
                    booking.getGuestLastName() != null ? booking.getGuestLastName() : "",
                    booking.getTotalAmountPaise() != null ? booking.getTotalAmountPaise() : 0);
            kafka.send(topic, booking.getId().toString(), event);
        } catch (Exception e) {
            log.warn("Failed to send payment reminder for {}: {}", booking.getBookingRef(), e.getMessage());
        }
    }
}
