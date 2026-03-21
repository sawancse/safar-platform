package com.safar.notification.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safar.notification.dto.EmailContext;
import com.safar.notification.service.BookingClient;
import com.safar.notification.service.EmailContextBuilder;
import com.safar.notification.service.NotificationService;
import com.safar.notification.service.UserClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final NotificationService notificationService;
    private final BookingClient bookingClient;
    private final UserClient userClient;
    private final EmailContextBuilder emailContextBuilder;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = {"payment.captured", "payment.failed", "payment.refunded",
                      "payment.reminder", "payment.reminder.urgent"},
            groupId = "notification-payment-group"
    )
    public void onPaymentEvent(String message,
                               @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        try {
            switch (topic) {
                case "payment.captured"        -> handlePaymentCaptured(message);
                case "payment.failed"          -> handlePaymentFailed(message);
                case "payment.refunded"        -> handlePaymentRefunded(message);
                case "payment.reminder"        -> handlePaymentReminder(message, false);
                case "payment.reminder.urgent" -> handlePaymentReminder(message, true);
                default -> log.warn("Unhandled payment topic: {}", topic);
            }
        } catch (Exception e) {
            log.error("Error handling payment event on topic {}: {}", topic, e.getMessage());
        }
    }

    /**
     * payment.captured → plain text notification + Chapter 2 (Payment Receipt) HTML email.
     * Attempts to enrich EmailContext with payment amounts from the event JSON.
     */
    private void handlePaymentCaptured(String message) {
        String bookingId = extractBookingId(message);

        // Send plain text + Chapter 2 via NotificationService
        notificationService.notifyPaymentCaptured(bookingId);

        // Try to enrich with payment amounts from the event and send Chapter 2 with full context
        try {
            JsonNode node = tryParseJson(message);
            if (node != null) {
                BookingClient.BookingInfo booking = bookingClient.getBooking(bookingId);
                if (booking != null) {
                    UserClient.UserInfo guest = userClient.getUser(booking.guestId());
                    UserClient.UserInfo host = userClient.getUser(booking.hostId());
                    EmailContext ctx = emailContextBuilder.buildBookingContext(booking, guest, host, null, null);

                    // Enrich with payment amounts from event if available
                    enrichPaymentContext(ctx, node);

                    UUID guestUuid = UUID.fromString(booking.guestId());
                    UUID hostUuid = parseUuidSafe(booking.hostId());
                    UUID listingUuid = parseUuidSafe(booking.listingId());
                    String guestEmail = resolveEmail(booking, guest);

                    // Chapter 2 is already sent by notifyPaymentCaptured's sendPaymentConfirmationHtml.
                    // The enriched context send is only needed if the event carries extra payment data
                    // that wasn't available to the base method. Since JourneyChapterService deduplicates,
                    // this is safe — the first send wins.
                    if (hasPaymentAmounts(node)) {
                        notificationService.sendPaymentConfirmationWithContext(
                                bookingId, ctx, guestEmail, guestUuid, hostUuid, listingUuid);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Could not enrich payment.captured with event data for booking {}: {}", bookingId, e.getMessage());
        }
    }

    /**
     * payment.failed → plain text + HTML payment-failed template.
     */
    private void handlePaymentFailed(String message) {
        String bookingId = extractBookingId(message);
        notificationService.notifyPaymentFailed(bookingId);
    }

    private void handlePaymentRefunded(String message) {
        String bookingId = extractBookingId(message);
        notificationService.notifyPaymentRefunded(bookingId);
    }

    private void handlePaymentReminder(String message, boolean urgent) {
        String bookingId = extractBookingId(message);
        // Extract guest info from event JSON if available
        JsonNode node = tryParseJson(message);
        String guestEmail = node != null && node.has("guestEmail") ? node.get("guestEmail").asText("") : "";
        String guestName = node != null && node.has("guestName") ? node.get("guestName").asText("") : "";
        String bookingRef = node != null && node.has("bookingRef") ? node.get("bookingRef").asText("") : "";
        long totalPaise = node != null && node.has("totalAmountPaise") ? node.get("totalAmountPaise").asLong(0) : 0;

        notificationService.notifyPaymentReminder(bookingId, guestEmail, guestName, bookingRef, totalPaise, urgent);
    }

    // ────────────────────────────────────────────────────────────
    // Helpers
    // ────────────────────────────────────────────────────────────

    private String extractBookingId(String message) {
        if (message == null) return "";
        String trimmed = message.trim();
        if (trimmed.startsWith("{")) {
            try {
                JsonNode node = objectMapper.readTree(trimmed);
                if (node.has("bookingId")) return node.get("bookingId").asText();
                if (node.has("id")) return node.get("id").asText();
            } catch (Exception e) {
                log.warn("Failed to parse payment event JSON, treating as plain bookingId: {}", e.getMessage());
            }
        }
        return trimmed;
    }

    private JsonNode tryParseJson(String message) {
        if (message == null || !message.trim().startsWith("{")) return null;
        try {
            return objectMapper.readTree(message);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Enrich EmailContext with payment amount fields from the event JSON.
     * All amounts in the event are expected to be in paise.
     */
    private void enrichPaymentContext(EmailContext ctx, JsonNode node) {
        if (node.has("totalAmountPaise")) {
            ctx.setTotalAmount(EmailContextBuilder.formatPaiseToRupeesWithSymbol(node.get("totalAmountPaise").asLong()));
        }
        if (node.has("baseAmountPaise")) {
            ctx.setBaseAmount(EmailContextBuilder.formatPaiseToRupeesWithSymbol(node.get("baseAmountPaise").asLong()));
        }
        if (node.has("gstAmountPaise")) {
            ctx.setGstAmount(EmailContextBuilder.formatPaiseToRupeesWithSymbol(node.get("gstAmountPaise").asLong()));
        }
        if (node.has("cleaningFeePaise")) {
            ctx.setCleaningFee(EmailContextBuilder.formatPaiseToRupeesWithSymbol(node.get("cleaningFeePaise").asLong()));
        }
        if (node.has("platformFeePaise")) {
            ctx.setPlatformFee(EmailContextBuilder.formatPaiseToRupeesWithSymbol(node.get("platformFeePaise").asLong()));
        }
        if (node.has("paymentMode")) {
            ctx.setPaymentMode(node.get("paymentMode").asText());
        }
        if (node.has("nonRefundable")) {
            ctx.setNonRefundable(node.get("nonRefundable").asBoolean());
        }
        if (node.has("nonRefundableDiscountPaise")) {
            ctx.setNonRefundableDiscount(EmailContextBuilder.formatPaiseToRupeesWithSymbol(node.get("nonRefundableDiscountPaise").asLong()));
        }
    }

    private boolean hasPaymentAmounts(JsonNode node) {
        return node.has("totalAmountPaise") || node.has("baseAmountPaise");
    }

    private String resolveEmail(BookingClient.BookingInfo booking, UserClient.UserInfo guest) {
        if (booking.guestEmail() != null && !booking.guestEmail().isBlank()) {
            return booking.guestEmail();
        }
        return guest != null ? guest.email() : "";
    }

    private static UUID parseUuidSafe(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
