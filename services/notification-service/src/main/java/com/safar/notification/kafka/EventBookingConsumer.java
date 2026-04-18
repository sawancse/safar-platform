package com.safar.notification.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safar.notification.dto.EmailContext;
import com.safar.notification.service.EmailGateway;
import com.safar.notification.service.EmailTemplateService;
import com.safar.notification.service.InAppNotificationService;
import com.safar.notification.service.UserClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Handles Kafka events from chef-service for Safar Cooks event booking notifications.
 * Both chef and customer receive HTML email + in-app notifications.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EventBookingConsumer {

    private final InAppNotificationService inAppNotificationService;
    private final EmailGateway emailGateway;
    private final EmailTemplateService emailTemplateService;
    private final UserClient userClient;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = {"event.booking.created", "event.booking.quoted",
                      "event.booking.confirmed", "event.booking.advance.paid",
                      "event.booking.completed", "event.booking.cancelled"},
            groupId = "notification-event-group"
    )
    public void onEventBookingEvent(String message,
                                     @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        try {
            JsonNode node = objectMapper.readTree(message);
            String bookingId = txt(node, "bookingId", message.trim());
            String bookingRef = txt(node, "bookingRef", bookingId);
            String chefId = txt(node, "chefId", null);
            String customerId = txt(node, "customerId", null);
            String chefName = txt(node, "chefName", "Chef");
            String rawCustomerName = txt(node, "customerName", "Customer");
            // The event's `customerName` holds the *guest* name the booker typed
            // (e.g. "sawan kumar" whose birthday it is), not the account holder.
            // For email greetings we want the actual account holder so the
            // greeting isn't "Hi, sawan kumar" when Gunja is the real recipient.
            String customerName = rawCustomerName;
            if (customerId != null) {
                try {
                    UserClient.UserInfo u = userClient.getUser(customerId);
                    if (u != null && u.name() != null && !u.name().isBlank()) {
                        customerName = u.name();
                    }
                } catch (Exception ignored) { /* fall back to raw name */ }
            }
            String eventType = txt(node, "eventType", "");
            String eventDate = txt(node, "eventDate", "");
            String eventTime = txt(node, "eventTime", "");
            String venueAddress = txt(node, "venueAddress", "");
            String city = txt(node, "city", "");
            String status = txt(node, "status", "");
            int guestCount = node.has("guestCount") ? node.get("guestCount").asInt() : 0;
            int durationHours = node.has("durationHours") ? node.get("durationHours").asInt() : 0;
            long totalPaise = node.has("totalAmountPaise") ? node.get("totalAmountPaise").asLong() : 0;
            long advancePaise = node.has("advanceAmountPaise") ? node.get("advanceAmountPaise").asLong() : 0;
            long balancePaise = node.has("balanceAmountPaise") ? node.get("balanceAmountPaise").asLong() : 0;

            String amount = formatINR(totalPaise);

            // Build shared context
            EmailContext ctx = new EmailContext();
            ctx.setBookingId(bookingId);
            ctx.setBookingRef(bookingRef);
            ctx.setChefName(chefName);
            ctx.setCustomerName(customerName);
            ctx.setServiceDate(eventDate);
            ctx.setEventType(eventType);
            ctx.setEventTime(eventTime);
            ctx.setEventGuestCount(guestCount);
            ctx.setGuestCount(guestCount);
            ctx.setVenueAddress(venueAddress);
            ctx.setCity(city);
            ctx.setTotalAmount(amount);
            ctx.setAdvanceAmount(formatINR(advancePaise));
            ctx.setBalanceAmount(formatINR(balancePaise));
            ctx.setDuration(durationHours + " hours");

            switch (topic) {
                case "event.booking.created" -> handleCreated(bookingId, customerId, chefId, chefName, customerName, bookingRef, eventType, eventDate, amount, ctx);
                case "event.booking.quoted" -> handleQuoted(bookingId, customerId, chefId, chefName, customerName, bookingRef, eventType, amount, ctx);
                case "event.booking.confirmed" -> handleConfirmed(bookingId, customerId, chefId, chefName, customerName, bookingRef, eventType, eventDate, amount, ctx);
                case "event.booking.advance.paid" -> handleAdvancePaid(bookingId, customerId, chefId, chefName, customerName, bookingRef, eventType, eventDate, amount, ctx);
                case "event.booking.completed" -> handleCompleted(bookingId, customerId, chefId, chefName, customerName, bookingRef, eventType, amount, ctx);
                case "event.booking.cancelled" -> handleCancelled(bookingId, customerId, chefId, chefName, customerName, bookingRef, eventType, ctx);
                default -> log.warn("Unhandled event booking topic: {}", topic);
            }
        } catch (Exception e) {
            log.error("Error handling event booking event on topic {}: {}", topic, e.getMessage());
        }
    }

    // ── Event Created ────────────────────────────────────────────────────

    private void handleCreated(String bookingId, String customerId, String chefId,
                                String chefName, String customerName, String bookingRef,
                                String eventType, String eventDate, String amount, EmailContext ctx) {
        if (customerId != null) {
            inAppNotificationService.create(
                    UUID.fromString(customerId),
                    "Event Inquiry Submitted",
                    "Your " + eventType + " event inquiry " + bookingRef + " for " + eventDate + " has been submitted. We'll match you with the perfect chef.",
                    "EVENT_BOOKING_CREATED", bookingId, "EVENT_BOOKING");

            sendHtmlEmail(customerId, "Event Inquiry Submitted — " + bookingRef,
                    "event-booking-created", ctx);
        }

        log.info("Event booking created notifications sent: {} customer={}", bookingRef, customerId);
    }

    // ── Event Quoted ─────────────────────────────────────────────────────

    private void handleQuoted(String bookingId, String customerId, String chefId,
                               String chefName, String customerName, String bookingRef,
                               String eventType, String amount, EmailContext ctx) {
        if (customerId != null) {
            inAppNotificationService.create(
                    UUID.fromString(customerId),
                    "Your Quote is Ready!",
                    chefName + " has sent a quote of " + amount + " for your " + eventType + " event " + bookingRef + ". Review and confirm!",
                    "EVENT_BOOKING_QUOTED", bookingId, "EVENT_BOOKING");

            sendHtmlEmail(customerId, "Your Quote is Ready — " + bookingRef,
                    "event-booking-quoted", ctx);
        }

        log.info("Event booking quoted notifications sent: {} customer={}", bookingRef, customerId);
    }

    // ── Event Confirmed ──────────────────────────────────────────────────

    private void handleConfirmed(String bookingId, String customerId, String chefId,
                                  String chefName, String customerName, String bookingRef,
                                  String eventType, String eventDate, String amount, EmailContext ctx) {
        if (customerId != null) {
            inAppNotificationService.create(
                    UUID.fromString(customerId),
                    "Event Confirmed!",
                    "Your " + eventType + " event " + bookingRef + " with " + chefName + " for " + eventDate + " is confirmed! Total: " + amount,
                    "EVENT_BOOKING_CONFIRMED", bookingId, "EVENT_BOOKING");

            sendHtmlEmail(customerId, "Event Confirmed — " + bookingRef,
                    "event-booking-confirmed", ctx);
        }

        if (chefId != null) {
            inAppNotificationService.create(
                    UUID.fromString(chefId),
                    "Event Confirmed",
                    "Event " + bookingRef + " for " + customerName + " on " + eventDate + " is confirmed. Total: " + amount,
                    "EVENT_BOOKING_CONFIRMED", bookingId, "EVENT_BOOKING");

            sendHtmlEmail(chefId, "Event Confirmed — " + bookingRef,
                    "event-booking-confirmed", ctx);
        }

        log.info("Event booking confirmed notifications sent: {}", bookingRef);
    }

    // ── Advance Paid ─────────────────────────────────────────────────────

    private void handleAdvancePaid(String bookingId, String customerId, String chefId,
                                    String chefName, String customerName, String bookingRef,
                                    String eventType, String eventDate, String amount, EmailContext ctx) {
        if (customerId != null) {
            inAppNotificationService.create(
                    UUID.fromString(customerId),
                    "Payment Received",
                    "Advance payment of " + ctx.getAdvanceAmount() + " for event " + bookingRef + " has been received. Balance due: " + ctx.getBalanceAmount(),
                    "EVENT_BOOKING_ADVANCE_PAID", bookingId, "EVENT_BOOKING");

            sendHtmlEmail(customerId, "Payment Received — " + bookingRef,
                    "event-booking-advance-paid", ctx);
        }

        if (chefId != null) {
            inAppNotificationService.create(
                    UUID.fromString(chefId),
                    "Advance Payment Received",
                    "Advance of " + ctx.getAdvanceAmount() + " received for event " + bookingRef + " from " + customerName + ".",
                    "EVENT_BOOKING_ADVANCE_PAID", bookingId, "EVENT_BOOKING");

            sendHtmlEmail(chefId, "Advance Payment Received — " + bookingRef,
                    "event-booking-advance-paid", ctx);
        }

        log.info("Event booking advance paid notifications sent: {}", bookingRef);
    }

    // ── Event Completed ──────────────────────────────────────────────────

    private void handleCompleted(String bookingId, String customerId, String chefId,
                                  String chefName, String customerName, String bookingRef,
                                  String eventType, String amount, EmailContext ctx) {
        ctx.setChefEarnings(amount);

        if (customerId != null) {
            inAppNotificationService.create(
                    UUID.fromString(customerId),
                    "Event Complete!",
                    "Your " + eventType + " event " + bookingRef + " is done. How was the experience? Rate now!",
                    "EVENT_BOOKING_COMPLETED", bookingId, "EVENT_BOOKING");

            sendHtmlEmail(customerId, "How was your event? — " + bookingRef,
                    "event-booking-completed", ctx);
        }

        if (chefId != null) {
            inAppNotificationService.create(
                    UUID.fromString(chefId),
                    "Event Completed",
                    "Event " + bookingRef + " complete. Earnings: " + amount + " added to your balance.",
                    "EVENT_BOOKING_COMPLETED", bookingId, "EVENT_BOOKING");
        }

        log.info("Event booking completed notifications sent: {}", bookingRef);
    }

    // ── Event Cancelled ──────────────────────────────────────────────────

    private void handleCancelled(String bookingId, String customerId, String chefId,
                                  String chefName, String customerName, String bookingRef,
                                  String eventType, EmailContext ctx) {
        if (customerId != null) {
            inAppNotificationService.create(
                    UUID.fromString(customerId),
                    "Event Cancelled",
                    "Your " + eventType + " event " + bookingRef + " has been cancelled.",
                    "EVENT_BOOKING_CANCELLED", bookingId, "EVENT_BOOKING");

            sendHtmlEmail(customerId, "Event Cancelled — " + bookingRef,
                    "event-booking-cancelled", ctx);
        }

        if (chefId != null) {
            inAppNotificationService.create(
                    UUID.fromString(chefId),
                    "Event Cancelled",
                    "Event " + bookingRef + " with " + customerName + " has been cancelled.",
                    "EVENT_BOOKING_CANCELLED", bookingId, "EVENT_BOOKING");

            sendHtmlEmail(chefId, "Event Cancelled — " + bookingRef,
                    "event-booking-cancelled", ctx);
        }

        log.info("Event booking cancelled notifications sent: {}", bookingRef);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void sendHtmlEmail(String userId, String subject, String templateName, EmailContext ctx) {
        try {
            UserClient.UserInfo user = userClient.getUser(userId);
            if (user != null && user.email() != null && !user.email().isBlank()) {
                emailTemplateService.sendHtmlEmail(user.email(), subject, templateName, ctx);
            }
        } catch (Exception e) {
            log.warn("HTML email failed for template {}, user {}: {}", templateName, userId, e.getMessage());
            try {
                UserClient.UserInfo user = userClient.getUser(userId);
                if (user != null && user.email() != null && !user.email().isBlank()) {
                    emailGateway.send(user.email(), subject,
                            "Event " + ctx.getBookingRef() + " — " + subject
                            + "\n\nEvent Type: " + ctx.getEventType()
                            + "\nDate: " + ctx.getServiceDate()
                            + "\nAmount: " + ctx.getTotalAmount()
                            + "\n\nView details: https://ysafar.com/cooks/my-bookings"
                            + "\n\nSafar Cooks Team");
                }
            } catch (Exception e2) {
                log.error("Plain text email also failed for user {}: {}", userId, e2.getMessage());
            }
        }
    }

    private String txt(JsonNode node, String field, String defaultVal) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : defaultVal;
    }

    private String formatINR(long paise) {
        return "\u20B9" + String.format("%,d", paise / 100);
    }
}
