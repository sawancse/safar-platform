package com.safar.notification.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safar.notification.dto.EmailContext;
import com.safar.notification.service.EmailContextBuilder;
import com.safar.notification.service.EmailTemplateService;
import com.safar.notification.service.InAppNotificationService;
import com.safar.notification.service.SmsService;
import com.safar.notification.service.UserClient;
import com.safar.notification.service.WhatsAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Consumes flight booking lifecycle events and sends Email + SMS + In-App notifications.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FlightEventConsumer {

    private final EmailTemplateService emailTemplateService;
    private final InAppNotificationService inAppNotificationService;
    private final SmsService smsService;
    private final WhatsAppService whatsAppService;
    private final UserClient userClient;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = {"flight.booking.created", "flight.booking.confirmed", "flight.booking.cancelled",
                      "flight.booking.expired", "flight.reminder.checkin"},
            groupId = "notification-flight-group"
    )
    public void onFlightEvent(String message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        try {
            switch (topic) {
                case "flight.booking.created"    -> handleBookingCreated(message);
                case "flight.booking.confirmed"  -> handleBookingConfirmed(message);
                case "flight.booking.cancelled"  -> handleBookingCancelled(message);
                case "flight.booking.expired"    -> handleBookingExpired(message);
                case "flight.reminder.checkin"   -> handleCheckinReminder(message);
                default -> log.warn("Unhandled flight topic: {}", topic);
            }
        } catch (Exception e) {
            log.error("Error handling flight event on topic {}: {}", topic, e.getMessage(), e);
        }
    }

    // ── flight.booking.confirmed → E-ticket confirmation (Email + SMS + In-App) ──

    private void handleBookingConfirmed(String message) {
        JsonNode node = parse(message);
        if (node == null) return;

        String userId = node.path("userId").asText("");
        String email = node.path("contactEmail").asText("");
        String phone = node.path("contactPhone").asText("");
        String bookingRef = node.path("bookingRef").asText("");
        String route = node.path("departureCityCode").asText("") + " → " + node.path("arrivalCityCode").asText("");
        String amount = formatPaise(node.path("totalAmountPaise").asLong());
        String airline = node.path("airline").asText("");
        String flightNum = node.path("flightNumber").asText("");
        String depDate = node.path("departureDate").asText("");

        // Email
        if (!email.isBlank()) {
            UserClient.UserInfo user = !userId.isBlank() ? userClient.getUser(userId) : null;
            String name = user != null ? user.name() : "Traveller";

            EmailContext ctx = new EmailContext();
            ctx.setGuestName(name);
            ctx.setBookingRef(bookingRef);
            ctx.setFlightRoute(route);
            ctx.setFlightDate(depDate);
            ctx.setAirline(airline);
            ctx.setFlightNumber(flightNum);
            ctx.setTotalAmount(amount);
            ctx.setIsInternational(node.path("isInternational").asBoolean(false));

            emailTemplateService.sendHtmlEmail(email,
                    "Flight Confirmed! " + bookingRef + " — " + route,
                    "flight-booking-confirmed", ctx);
            log.info("Sent flight confirmation email to {} for {}", email, bookingRef);
        }

        // SMS
        if (!phone.isBlank()) {
            smsService.sendFlightConfirmation(phone, bookingRef, route, depDate, airline + " " + flightNum);
        }

        // WhatsApp (e-ticket)
        if (!phone.isBlank()) {
            try {
                whatsAppService.sendFlightBookingConfirmed(phone, bookingRef, route, depDate, airline + " " + flightNum);
            } catch (Exception e) {
                log.warn("WA flight confirmation failed: {}", e.getMessage());
            }
        }

        // In-App
        if (!userId.isBlank()) {
            inAppNotificationService.create(UUID.fromString(userId),
                    "Flight Confirmed",
                    "Your flight " + flightNum + " " + route + " on " + depDate + " is confirmed. Ref: " + bookingRef,
                    "FLIGHT_CONFIRMED", node.path("bookingId").asText(""), "FLIGHT_BOOKING");
        }
    }

    // ── flight.booking.created → Payment reminder (Email + SMS) ──

    private void handleBookingCreated(String message) {
        JsonNode node = parse(message);
        if (node == null) return;

        String userId = node.path("userId").asText("");
        String email = node.path("contactEmail").asText("");
        String phone = node.path("contactPhone").asText("");
        String bookingRef = node.path("bookingRef").asText("");
        String route = node.path("departureCityCode").asText("") + " → " + node.path("arrivalCityCode").asText("");
        String amount = formatPaise(node.path("totalAmountPaise").asLong());

        // In-App only — payment reminder
        if (!userId.isBlank()) {
            inAppNotificationService.create(UUID.fromString(userId),
                    "Complete Payment",
                    "Complete payment of " + amount + " for your flight " + route + " within 30 minutes. Ref: " + bookingRef,
                    "FLIGHT_PAYMENT_PENDING", node.path("bookingId").asText(""), "FLIGHT_BOOKING");
        }
    }

    // ── flight.booking.cancelled → Cancellation + refund notification ──

    private void handleBookingCancelled(String message) {
        JsonNode node = parse(message);
        if (node == null) return;

        String userId = node.path("userId").asText("");
        String email = node.path("contactEmail").asText("");
        String phone = node.path("contactPhone").asText("");
        String bookingRef = node.path("bookingRef").asText("");
        String route = node.path("departureCityCode").asText("") + " → " + node.path("arrivalCityCode").asText("");
        long refundPaise = node.path("refundAmountPaise").asLong(0);
        String refundAmount = refundPaise > 0 ? formatPaise(refundPaise) : "N/A";

        // Email
        if (!email.isBlank()) {
            UserClient.UserInfo user = !userId.isBlank() ? userClient.getUser(userId) : null;
            String name = user != null ? user.name() : "Traveller";

            EmailContext ctx = new EmailContext();
            ctx.setGuestName(name);
            ctx.setBookingRef(bookingRef);
            ctx.setFlightRoute(route);
            ctx.setRefundAmount(refundAmount);

            emailTemplateService.sendHtmlEmail(email,
                    "Flight Cancelled — " + bookingRef,
                    "flight-booking-cancelled", ctx);
            log.info("Sent flight cancellation email to {} for {}", email, bookingRef);
        }

        // SMS
        if (!phone.isBlank()) {
            smsService.sendFlightCancellation(phone, bookingRef, route, refundAmount);
        }

        // In-App
        if (!userId.isBlank()) {
            String msg = "Your flight " + route + " has been cancelled. Ref: " + bookingRef;
            if (refundPaise > 0) msg += ". Refund of " + refundAmount + " initiated.";
            inAppNotificationService.create(UUID.fromString(userId),
                    "Flight Cancelled", msg,
                    "FLIGHT_CANCELLED", node.path("bookingId").asText(""), "FLIGHT_BOOKING");
        }
    }

    // ── flight.booking.expired → Auto-cancel notification ──

    private void handleBookingExpired(String message) {
        JsonNode node = parse(message);
        if (node == null) return;

        String userId = node.path("userId").asText("");
        String bookingRef = node.path("bookingRef").asText("");
        String msgText = node.path("message").asText("Your flight booking expired due to incomplete payment.");

        if (!userId.isBlank()) {
            inAppNotificationService.create(UUID.fromString(userId),
                    "Booking Expired", msgText,
                    "FLIGHT_EXPIRED", node.path("bookingId").asText(""), "FLIGHT_BOOKING");
        }
    }

    // ── flight.reminder.checkin → Check-in + trip reminder ──

    private void handleCheckinReminder(String message) {
        JsonNode node = parse(message);
        if (node == null) return;

        String userId = node.path("userId").asText("");
        String email = node.path("contactEmail").asText("");
        String phone = node.path("contactPhone").asText("");
        String bookingRef = node.path("bookingRef").asText("");
        String route = node.path("departureCityCode").asText("") + " → " + node.path("arrivalCityCode").asText("");
        String airline = node.path("airline").asText("");
        String flightNum = node.path("flightNumber").asText("");
        String depDate = node.path("departureDate").asText("");

        // Email
        if (!email.isBlank()) {
            UserClient.UserInfo user = !userId.isBlank() ? userClient.getUser(userId) : null;
            String name = user != null ? user.name() : "Traveller";

            EmailContext ctx = new EmailContext();
            ctx.setGuestName(name);
            ctx.setBookingRef(bookingRef);
            ctx.setFlightRoute(route);
            ctx.setFlightDate(depDate);
            ctx.setAirline(airline);
            ctx.setFlightNumber(flightNum);

            emailTemplateService.sendHtmlEmail(email,
                    "Check-in Open! Your flight " + route + " is tomorrow",
                    "flight-checkin-reminder", ctx);
            log.info("Sent check-in reminder email to {} for {}", email, bookingRef);
        }

        // SMS
        if (!phone.isBlank()) {
            smsService.sendFlightCheckinReminder(phone, bookingRef, route, airline + " " + flightNum);
        }

        // In-App
        if (!userId.isBlank()) {
            inAppNotificationService.create(UUID.fromString(userId),
                    "Check-in Open",
                    "Web check-in is now open for your flight " + flightNum + " " + route + " tomorrow. Ref: " + bookingRef,
                    "FLIGHT_CHECKIN_REMINDER", node.path("bookingId").asText(""), "FLIGHT_BOOKING");
        }
    }

    private JsonNode parse(String message) {
        try { return objectMapper.readTree(message); }
        catch (Exception e) { log.warn("Failed to parse flight event: {}", e.getMessage()); return null; }
    }

    private String formatPaise(long paise) {
        return EmailContextBuilder.formatPaiseToRupeesWithSymbol(paise);
    }
}
