package com.safar.notification.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safar.notification.dto.EmailContext;
import com.safar.notification.service.EmailTemplateService;
import com.safar.notification.service.InAppNotificationService;
import com.safar.notification.service.BookingClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Consumes {@code booking.balance.reminder} events from booking-service for
 * PARTIAL_PREPAID bookings whose online balance is still outstanding.
 *
 * Sends an email (template: balance-payment-reminder) + an in-app notification.
 * Tier-aware subject: T-7 is friendly, T-1 is urgent.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BookingBalanceReminderConsumer {

    private final EmailTemplateService emailTemplateService;
    private final InAppNotificationService inAppNotificationService;
    private final BookingClient bookingClient;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "booking.balance.reminder", groupId = "notification-balance-reminder-group")
    public void onBalanceReminder(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);

            String bookingId = node.path("bookingId").asText("");
            String bookingRef = node.path("bookingRef").asText("");
            String tier = node.path("tier").asText("T7");
            long duePaise = node.path("dueAtPropertyPaise").asLong(0);
            long totalPaise = node.path("totalAmountPaise").asLong(0);
            String checkIn = node.path("checkIn").asText("");
            String guestEmail = node.path("guestEmail").asText("");
            String guestName = node.path("guestName").asText("Guest");
            String listingTitle = node.path("listingTitle").asText("");
            boolean urgent = "T1".equals(tier);

            if (bookingId.isBlank()) {
                log.warn("booking.balance.reminder event missing bookingId — skipping");
                return;
            }

            // Fall back to booking-service lookup if the event didn't carry an email
            if (guestEmail.isBlank()) {
                BookingClient.BookingInfo b = bookingClient.getBooking(bookingId);
                if (b != null) {
                    guestEmail = b.guestEmail();
                    if (guestName.isBlank() || "Guest".equals(guestName)) guestName = b.guestName();
                    if (bookingRef.isBlank()) bookingRef = b.bookingRef();
                }
            }

            String duePretty = "₹" + String.format("%,d", duePaise / 100);
            String totalPretty = totalPaise > 0 ? "₹" + String.format("%,d", totalPaise / 100) : "";
            String countdown = urgent ? "tomorrow" : "in 7 days";

            if (guestEmail != null && !guestEmail.isBlank()) {
                EmailContext ctx = new EmailContext();
                ctx.setGuestName(guestName);
                ctx.setBookingId(bookingId);
                ctx.setBookingRef(bookingRef);
                ctx.setCheckIn(checkIn);
                ctx.setListingTitle(listingTitle);
                ctx.setDueAtProperty(duePretty);
                ctx.setTotalAmount(totalPretty);
                ctx.setCountdownText(countdown);
                ctx.setPaymentMode("PARTIAL_PREPAID");

                String subject = urgent
                        ? "Final reminder: pay balance " + duePretty + " for " + bookingRef
                        : "Pay your remaining balance — " + bookingRef;

                emailTemplateService.sendHtmlEmail(guestEmail, subject, "balance-payment-reminder", ctx);
                log.info("Balance reminder ({}) email sent to {} for booking {}", tier, guestEmail, bookingRef);
            }

            // In-app notification (guest UUID is the booking's guestId — fetch if absent)
            try {
                BookingClient.BookingInfo b = bookingClient.getBooking(bookingId);
                if (b != null && b.guestId() != null) {
                    String body = "Balance " + duePretty + " is due " + countdown + " for " + bookingRef + ".";
                    inAppNotificationService.create(UUID.fromString(b.guestId()),
                            urgent ? "Pay balance tomorrow" : "Balance reminder",
                            body,
                            urgent ? "BALANCE_REMINDER_URGENT" : "BALANCE_REMINDER",
                            bookingId, "PAYMENT");
                }
            } catch (Exception e) {
                log.warn("In-app balance reminder failed for {}: {}", bookingRef, e.getMessage());
            }

        } catch (Exception e) {
            log.error("Failed to handle booking.balance.reminder: {}", e.getMessage(), e);
        }
    }
}
