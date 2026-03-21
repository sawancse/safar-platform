package com.safar.notification.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safar.notification.dto.EmailContext;
import com.safar.notification.service.BookingClient;
import com.safar.notification.service.EmailContextBuilder;
import com.safar.notification.service.EmailSchedulerService;
import com.safar.notification.service.NotificationService;
import com.safar.notification.service.UserClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingEventConsumer {

    private final NotificationService notificationService;
    private final BookingClient bookingClient;
    private final UserClient userClient;
    private final EmailContextBuilder emailContextBuilder;
    private final EmailSchedulerService emailSchedulerService;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter ISO_DT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    @KafkaListener(
            topics = {"booking.created", "booking.confirmed", "booking.cancelled",
                      "booking.checked-in", "booking.completed", "booking.expired"},
            groupId = "notification-booking-group"
    )
    public void onBookingEvent(String message,
                               @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        try {
            switch (topic) {
                case "booking.created"    -> handleBookingCreated(message);
                case "booking.confirmed"  -> handleBookingConfirmed(message);
                case "booking.cancelled"  -> handleBookingCancelled(message);
                case "booking.checked-in" -> handleBookingCheckedIn(message);
                case "booking.completed"  -> handleBookingCompleted(message);
                case "booking.expired"    -> handleBookingExpired(message);
                default -> log.warn("Unhandled booking topic: {}", topic);
            }
        } catch (Exception e) {
            log.error("Error handling booking event on topic {}: {}", topic, e.getMessage());
        }
    }

    /**
     * booking.created → plain text notifications + host HTML new-booking alert.
     * The message may be a plain bookingId string or a JSON object with extra fields.
     */
    private void handleBookingCreated(String message) {
        String bookingId = extractBookingId(message);
        notificationService.notifyBookingCreated(bookingId);
        // The host-new-booking HTML alert is already sent inside notifyBookingCreated
    }

    /**
     * booking.confirmed → plain text + Chapter 1 (Journey Unlocked) + schedule future chapters.
     * Attempts to parse check-in/check-out from the event JSON to schedule journey chapters.
     */
    private void handleBookingConfirmed(String message) {
        String bookingId = extractBookingId(message);

        // First: send the existing plain-text + Chapter 1 via NotificationService
        notificationService.notifyBookingConfirmed(bookingId);

        // Then: try to schedule remaining journey chapters using dates from the event
        try {
            LocalDateTime checkIn = null;
            LocalDateTime checkOut = null;
            try {
                JsonNode node = objectMapper.readTree(message);
                checkIn = parseDateTime(node, "checkIn");
                checkOut = parseDateTime(node, "checkOut");
            } catch (Exception ignored) {
                // message was a plain bookingId string — no JSON to parse
            }

            if (checkIn != null && checkOut != null) {
                UUID bookingUuid = UUID.fromString(bookingId);
                BookingClient.BookingInfo booking = bookingClient.getBooking(bookingId);
                if (booking != null) {
                    UUID guestUuid = UUID.fromString(booking.guestId());
                    emailSchedulerService.scheduleBookingJourneyEmails(bookingUuid, guestUuid, checkIn, checkOut);
                    log.info("Scheduled journey chapters for confirmed booking {} (check-in: {}, check-out: {})",
                            bookingId, checkIn, checkOut);
                }
            } else {
                log.debug("No check-in/check-out dates in booking.confirmed event for {} — journey chapters not scheduled from consumer", bookingId);
            }
        } catch (Exception e) {
            log.warn("Failed to schedule journey chapters for booking {}: {}", bookingId, e.getMessage());
        }
    }

    /**
     * booking.cancelled → plain text + HTML cancellation + cancel all scheduled emails.
     */
    private void handleBookingCancelled(String message) {
        String bookingId = extractBookingId(message);
        // notifyBookingCancelled already handles plain text, HTML cancellation, and cancelling scheduled emails
        notificationService.notifyBookingCancelled(bookingId);
    }

    private void handleBookingCheckedIn(String message) {
        String bookingId = extractBookingId(message);
        notificationService.notifyBookingCheckedIn(bookingId);
    }

    private void handleBookingCompleted(String message) {
        String bookingId = extractBookingId(message);
        notificationService.notifyBookingCompleted(bookingId);
    }

    private void handleBookingExpired(String message) {
        String bookingId = extractBookingId(message);
        notificationService.notifyBookingExpired(bookingId);
    }

    // ────────────────────────────────────────────────────────────
    // Helpers
    // ────────────────────────────────────────────────────────────

    /**
     * Extract bookingId from a message that may be a plain ID string or a JSON object.
     */
    private String extractBookingId(String message) {
        if (message == null) return "";
        String trimmed = message.trim();
        if (trimmed.startsWith("{")) {
            try {
                JsonNode node = objectMapper.readTree(trimmed);
                if (node.has("bookingId")) return node.get("bookingId").asText();
                if (node.has("id")) return node.get("id").asText();
            } catch (Exception e) {
                log.warn("Failed to parse booking event JSON, treating as plain bookingId: {}", e.getMessage());
            }
        }
        return trimmed;
    }

    /**
     * Parse a LocalDateTime from a JSON node field. Handles both ISO_LOCAL_DATE_TIME and ISO_LOCAL_DATE formats.
     */
    private LocalDateTime parseDateTime(JsonNode node, String field) {
        if (!node.has(field) || node.get(field).isNull()) return null;
        String value = node.get(field).asText();
        if (value.isBlank()) return null;
        try {
            return LocalDateTime.parse(value, ISO_DT);
        } catch (DateTimeParseException e1) {
            try {
                return LocalDateTime.parse(value + "T00:00:00", ISO_DT);
            } catch (DateTimeParseException e2) {
                log.warn("Cannot parse {} value '{}' as LocalDateTime", field, value);
                return null;
            }
        }
    }
}
