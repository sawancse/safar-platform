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
 * Handles Kafka events from chef-service for Safar Cooks notifications.
 * Both chef and customer receive HTML email + in-app notifications.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChefEventConsumer {

    private final InAppNotificationService inAppNotificationService;
    private final EmailGateway emailGateway;
    private final EmailTemplateService emailTemplateService;
    private final UserClient userClient;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = {"chef.booking.created", "chef.booking.confirmed",
                      "chef.booking.cancelled", "chef.booking.completed"},
            groupId = "notification-chef-group"
    )
    public void onChefBookingEvent(String message,
                                    @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        try {
            JsonNode node = objectMapper.readTree(message);
            String bookingId = txt(node, "bookingId", message.trim());
            String bookingRef = txt(node, "bookingRef", bookingId);
            String chefId = txt(node, "chefId", null);
            String customerId = txt(node, "customerId", null);
            String chefName = txt(node, "chefName", "Chef");
            String customerName = txt(node, "customerName", "Customer");
            String serviceDate = txt(node, "serviceDate", "");
            String mealType = txt(node, "mealType", "");
            String city = txt(node, "city", "");
            long totalPaise = node.has("totalAmountPaise") ? node.get("totalAmountPaise").asLong() : 0;
            String amount = formatINR(totalPaise);

            // Build shared context
            EmailContext ctx = new EmailContext();
            ctx.setBookingRef(bookingRef);
            ctx.setChefName(chefName);
            ctx.setCustomerName(customerName);
            ctx.setServiceDate(serviceDate);
            ctx.setMealType(mealType);
            ctx.setTotalAmount(amount);
            ctx.setCity(city);

            switch (topic) {
                case "chef.booking.created" -> handleCreated(bookingId, customerId, chefId, chefName, customerName, bookingRef, serviceDate, mealType, amount, ctx);
                case "chef.booking.confirmed" -> handleConfirmed(bookingId, customerId, chefId, chefName, customerName, bookingRef, serviceDate, mealType, amount, ctx);
                case "chef.booking.cancelled" -> handleCancelled(bookingId, customerId, chefId, chefName, customerName, bookingRef, serviceDate, mealType, ctx);
                case "chef.booking.completed" -> handleCompleted(bookingId, customerId, chefId, chefName, customerName, bookingRef, serviceDate, mealType, amount, ctx);
                default -> log.warn("Unhandled chef topic: {}", topic);
            }
        } catch (Exception e) {
            log.error("Error handling chef event on topic {}: {}", topic, e.getMessage());
        }
    }

    // ── Booking Created ──────────────────────────────────────────────────────

    private void handleCreated(String bookingId, String customerId, String chefId,
                                String chefName, String customerName, String bookingRef,
                                String serviceDate, String mealType, String amount, EmailContext ctx) {
        // Customer: in-app + HTML email
        if (customerId != null) {
            inAppNotificationService.create(
                    UUID.fromString(customerId),
                    "Cook Booking Placed",
                    "Your booking " + bookingRef + " with " + chefName + " for " + serviceDate + " is placed. Waiting for chef confirmation.",
                    "CHEF_BOOKING_CREATED", bookingId, "CHEF_BOOKING");

            sendHtmlEmail(customerId, "Booking Placed — " + bookingRef,
                    "chef-booking-created-customer", ctx);
        }

        // Chef: in-app + HTML email
        if (chefId != null) {
            inAppNotificationService.create(
                    UUID.fromString(chefId),
                    "New Booking Request",
                    "New booking " + bookingRef + " from " + customerName + " for " + serviceDate + " (" + mealType + "). Please confirm.",
                    "CHEF_BOOKING_CREATED", bookingId, "CHEF_BOOKING");

            sendHtmlEmail(chefId, "New Booking Request — " + bookingRef,
                    "chef-booking-created-chef", ctx);
        }

        log.info("Chef booking created notifications sent: {} customer={} chef={}", bookingRef, customerId, chefId);
    }

    // ── Booking Confirmed ────────────────────────────────────────────────────

    private void handleConfirmed(String bookingId, String customerId, String chefId,
                                  String chefName, String customerName, String bookingRef,
                                  String serviceDate, String mealType, String amount, EmailContext ctx) {
        if (customerId != null) {
            inAppNotificationService.create(
                    UUID.fromString(customerId),
                    "Booking Confirmed!",
                    chefName + " has confirmed your booking " + bookingRef + " for " + serviceDate + ". Your cook will arrive at the scheduled time.",
                    "CHEF_BOOKING_CONFIRMED", bookingId, "CHEF_BOOKING");

            sendHtmlEmail(customerId, "Booking Confirmed — " + bookingRef,
                    "chef-booking-confirmed", ctx);
        }

        if (chefId != null) {
            inAppNotificationService.create(
                    UUID.fromString(chefId),
                    "Booking Confirmed",
                    "You confirmed booking " + bookingRef + " with " + customerName + " for " + serviceDate + ". Earnings: " + amount,
                    "CHEF_BOOKING_CONFIRMED", bookingId, "CHEF_BOOKING");
        }

        log.info("Chef booking confirmed notifications sent: {}", bookingRef);
    }

    // ── Booking Cancelled ────────────────────────────────────────────────────

    private void handleCancelled(String bookingId, String customerId, String chefId,
                                  String chefName, String customerName, String bookingRef,
                                  String serviceDate, String mealType, EmailContext ctx) {
        if (customerId != null) {
            inAppNotificationService.create(
                    UUID.fromString(customerId),
                    "Booking Cancelled",
                    "Your cook booking " + bookingRef + " with " + chefName + " has been cancelled.",
                    "CHEF_BOOKING_CANCELLED", bookingId, "CHEF_BOOKING");

            sendHtmlEmail(customerId, "Booking Cancelled — " + bookingRef,
                    "chef-booking-cancelled", ctx);
        }

        if (chefId != null) {
            inAppNotificationService.create(
                    UUID.fromString(chefId),
                    "Booking Cancelled",
                    "Booking " + bookingRef + " with " + customerName + " for " + serviceDate + " has been cancelled.",
                    "CHEF_BOOKING_CANCELLED", bookingId, "CHEF_BOOKING");

            sendHtmlEmail(chefId, "Booking Cancelled — " + bookingRef,
                    "chef-booking-cancelled", ctx);
        }

        log.info("Chef booking cancelled notifications sent: {}", bookingRef);
    }

    // ── Booking Completed ────────────────────────────────────────────────────

    private void handleCompleted(String bookingId, String customerId, String chefId,
                                  String chefName, String customerName, String bookingRef,
                                  String serviceDate, String mealType, String amount, EmailContext ctx) {
        ctx.setChefEarnings(amount);

        if (customerId != null) {
            inAppNotificationService.create(
                    UUID.fromString(customerId),
                    "Meal Complete!",
                    "Your booking " + bookingRef + " with " + chefName + " is done. Rate your experience!",
                    "CHEF_BOOKING_COMPLETED", bookingId, "CHEF_BOOKING");

            sendHtmlEmail(customerId, "How was your meal? — " + bookingRef,
                    "chef-booking-completed", ctx);
        }

        if (chefId != null) {
            inAppNotificationService.create(
                    UUID.fromString(chefId),
                    "Booking Completed",
                    "Booking " + bookingRef + " complete. Earnings: " + amount + " added to your balance.",
                    "CHEF_BOOKING_COMPLETED", bookingId, "CHEF_BOOKING");
        }

        log.info("Chef booking completed notifications sent: {}", bookingRef);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void sendHtmlEmail(String userId, String subject, String templateName, EmailContext ctx) {
        try {
            UserClient.UserInfo user = userClient.getUser(userId);
            if (user != null && user.email() != null && !user.email().isBlank()) {
                emailTemplateService.sendHtmlEmail(user.email(), subject, templateName, ctx);
            }
        } catch (Exception e) {
            // Fallback to plain text if HTML template fails
            log.warn("HTML email failed for template {}, user {}: {}", templateName, userId, e.getMessage());
            try {
                UserClient.UserInfo user = userClient.getUser(userId);
                if (user != null && user.email() != null && !user.email().isBlank()) {
                    emailGateway.send(user.email(), subject,
                            "Booking " + ctx.getBookingRef() + " — " + subject
                            + "\n\nChef: " + ctx.getChefName()
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
        return "₹" + String.format("%,d", paise / 100);
    }
}
