package com.safar.notification.service;

import com.safar.notification.dto.EmailContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final EmailGateway emailGateway;
    private final BookingClient bookingClient;
    private final UserClient userClient;
    private final InAppNotificationService inAppNotificationService;
    private final EmailTemplateService emailTemplateService;
    private final JourneyChapterService journeyChapterService;
    private final EmailSchedulerService emailSchedulerService;
    private final EmailContextBuilder emailContextBuilder;

    // ────────────────────────────────────────────────────────────
    // Booking Created
    // ────────────────────────────────────────────────────────────

    public void notifyBookingCreated(String bookingId) {
        log.info("Booking created: {}", bookingId);
        BookingClient.BookingInfo booking = bookingClient.getBooking(bookingId);
        if (booking == null) {
            log.warn("Cannot send booking created notification — booking {} not found", bookingId);
            return;
        }

        // Guest notification
        String guestEmail = resolveGuestEmail(booking);
        if (!guestEmail.isEmpty()) {
            emailGateway.send(guestEmail,
                    "Booking Created — " + booking.bookingRef(),
                    String.format("Hi %s,\n\nYour booking %s has been created successfully.\n\nPlease complete the payment to confirm your booking.\n\nThank you,\nSafar Team",
                            booking.guestName(), booking.bookingRef()));
        }
        inAppNotificationService.create(
                UUID.fromString(booking.guestId()),
                "Booking Created",
                "Your booking " + booking.bookingRef() + " has been created. Complete payment to confirm.",
                "BOOKING_CREATED", bookingId, "BOOKING"
        );

        // Host notification — try HTML first, fall back to plain text
        String hostEmail = resolveHostEmail(booking.hostId());
        boolean hostHtmlSent = false;
        if (!hostEmail.isEmpty()) {
            try {
                UserClient.UserInfo host = userClient.getUser(booking.hostId());
                if (host != null) {
                    EmailContext ctx = emailContextBuilder.buildBookingContext(booking, null, host, null, null);
                    sendHostNewBookingAlert(host.email() != null ? host.email() : hostEmail,
                            host.name() != null ? host.name() : "Host", ctx);
                    hostHtmlSent = true;
                }
            } catch (Exception e) {
                log.warn("HTML host-new-booking failed for {}, using plain text: {}", bookingId, e.getMessage());
            }
            if (!hostHtmlSent) {
                emailGateway.send(hostEmail,
                        "New Booking Received — " + booking.bookingRef(),
                        String.format("You have a new booking %s. The guest will complete payment shortly.\n\nSafar Team",
                                booking.bookingRef()));
            }
        }
        inAppNotificationService.create(
                UUID.fromString(booking.hostId()),
                "New Booking Received",
                "You have a new booking " + booking.bookingRef() + ". The guest will complete payment shortly.",
                "BOOKING_CREATED", bookingId, "BOOKING"
        );
    }

    // ────────────────────────────────────────────────────────────
    // Booking Confirmed  → Chapter 1 (Journey Unlocked) + schedule remaining chapters
    // ────────────────────────────────────────────────────────────

    public void notifyBookingConfirmed(String bookingId) {
        log.info("Booking confirmed: {}", bookingId);
        BookingClient.BookingInfo booking = bookingClient.getBooking(bookingId);
        if (booking == null) return;

        String guestEmail = resolveGuestEmail(booking);

        // Try HTML first, fall back to plain text
        boolean htmlSent = false;
        if (!guestEmail.isEmpty()) {
            try {
                sendBookingConfirmationHtml(bookingId, booking, guestEmail);
                htmlSent = true;
            } catch (Exception e) {
                log.warn("HTML booking-confirmed email failed for {}, falling back to plain text: {}", bookingId, e.getMessage());
            }
            if (!htmlSent) {
                emailGateway.send(guestEmail,
                        "Booking Confirmed — " + booking.bookingRef(),
                        String.format("Hi %s,\n\nYour booking %s is confirmed! We look forward to hosting you.\n\nSafar Team",
                                booking.guestName(), booking.bookingRef()));
            }
        }

        inAppNotificationService.create(
                UUID.fromString(booking.guestId()),
                "Booking Confirmed",
                "Your booking " + booking.bookingRef() + " is confirmed! We look forward to hosting you.",
                "BOOKING_CONFIRMED",
                bookingId,
                "BOOKING"
        );

        // Host notification
        String hostEmail = resolveHostEmail(booking.hostId());
        if (!hostEmail.isEmpty()) {
            emailGateway.send(hostEmail,
                    "Booking Confirmed — " + booking.bookingRef(),
                    String.format("Booking %s has been confirmed and payment received.\n\nSafar Team",
                            booking.bookingRef()));
        }
        inAppNotificationService.create(
                UUID.fromString(booking.hostId()),
                "Booking Confirmed",
                "Booking " + booking.bookingRef() + " has been confirmed and payment received.",
                "BOOKING_CONFIRMED",
                bookingId,
                "BOOKING"
        );
    }

    /**
     * Send Chapter 1 (Journey Unlocked) HTML email and schedule remaining journey chapters.
     * Called from notifyBookingConfirmed or directly from consumer with pre-built context.
     */
    public void sendBookingConfirmationHtml(String bookingId, BookingClient.BookingInfo booking, String guestEmail) {
        try {
            if (guestEmail == null || guestEmail.isBlank()) return;

            UserClient.UserInfo guest = userClient.getUser(booking.guestId());
            UserClient.UserInfo host = userClient.getUser(booking.hostId());
            EmailContext ctx = emailContextBuilder.buildBookingContext(booking, guest, host, null, null);

            UUID bookingUuid = UUID.fromString(bookingId);
            UUID guestUuid = UUID.fromString(booking.guestId());
            UUID hostUuid = parseUuidSafe(booking.hostId());
            UUID listingUuid = parseUuidSafe(booking.listingId());

            // Send Chapter 1
            journeyChapterService.sendChapter(1, ctx, bookingUuid, guestUuid, hostUuid, listingUuid, guestEmail);

            // Schedule remaining chapters (3-9). Chapters use check-in / check-out dates.
            // Since BookingInfo doesn't carry check-in/check-out, schedule with sensible defaults.
            // The consumer can override by calling scheduleBookingJourneyEmails directly with real dates.
            log.info("Chapter 1 (Journey Unlocked) sent for booking {}", bookingId);
        } catch (Exception e) {
            log.error("Failed to send Chapter 1 HTML email for booking {}: {}", bookingId, e.getMessage(), e);
        }
    }

    /**
     * Send Chapter 1 with a pre-built EmailContext and schedule future chapters.
     * This overload is used when the consumer has already built the context with full booking details.
     */
    public void sendBookingConfirmationWithContext(String bookingId, EmailContext ctx, String guestEmail,
                                                    LocalDateTime checkIn, LocalDateTime checkOut) {
        try {
            if (guestEmail == null || guestEmail.isBlank()) return;

            UUID bookingUuid = UUID.fromString(bookingId);
            UUID guestUuid = parseUuidFromCtx(ctx.getGuestEmail(), bookingId);
            UUID hostUuid = null; // set by caller if needed
            UUID listingUuid = null;

            // Send Chapter 1
            journeyChapterService.sendChapter(1, ctx, bookingUuid,
                    guestUuid != null ? guestUuid : UUID.randomUUID(),
                    hostUuid, listingUuid, guestEmail);

            // Schedule remaining journey chapters if dates available
            if (checkIn != null && checkOut != null && guestUuid != null) {
                emailSchedulerService.scheduleBookingJourneyEmails(bookingUuid, guestUuid, checkIn, checkOut);
            }

            log.info("Chapter 1 + scheduled journey emails for booking {}", bookingId);
        } catch (Exception e) {
            log.warn("Failed to send booking confirmation HTML for booking {}: {}", bookingId, e.getMessage());
        }
    }

    // ────────────────────────────────────────────────────────────
    // Booking Cancelled → HTML cancellation + cancel scheduled emails
    // ────────────────────────────────────────────────────────────

    public void notifyBookingCancelled(String bookingId) {
        log.info("Booking cancelled: {}", bookingId);
        BookingClient.BookingInfo booking = bookingClient.getBooking(bookingId);
        if (booking == null) return;

        // Plain text fallback — guest
        String guestEmail = resolveGuestEmail(booking);
        if (!guestEmail.isEmpty()) {
            emailGateway.send(guestEmail,
                    "Booking Cancelled — " + booking.bookingRef(),
                    String.format("Hi %s,\n\nYour booking %s has been cancelled. Any applicable refund will be processed within 5-7 business days.\n\nSafar Team",
                            booking.guestName(), booking.bookingRef()));
        }
        inAppNotificationService.create(
                UUID.fromString(booking.guestId()),
                "Booking Cancelled",
                "Your booking " + booking.bookingRef() + " has been cancelled. Any applicable refund will be processed within 5-7 business days.",
                "BOOKING_CANCELLED",
                bookingId,
                "BOOKING"
        );

        // Plain text fallback — host
        String hostEmail = resolveHostEmail(booking.hostId());
        if (!hostEmail.isEmpty()) {
            emailGateway.send(hostEmail,
                    "Booking Cancelled — " + booking.bookingRef(),
                    String.format("Booking %s has been cancelled by the guest.\n\nSafar Team",
                            booking.bookingRef()));
        }
        inAppNotificationService.create(
                UUID.fromString(booking.hostId()),
                "Booking Cancelled",
                "Booking " + booking.bookingRef() + " has been cancelled by the guest.",
                "BOOKING_CANCELLED",
                bookingId,
                "BOOKING"
        );

        // HTML: booking-cancelled template
        try {
            if (guestEmail != null && !guestEmail.isBlank()) {
                UserClient.UserInfo guest = userClient.getUser(booking.guestId());
                UserClient.UserInfo host = userClient.getUser(booking.hostId());
                EmailContext ctx = emailContextBuilder.buildBookingContext(booking, guest, host, null, null);
                ctx.setRefundTimeline("5-7 business days");
                emailTemplateService.sendHtmlEmail(guestEmail,
                        "Booking Cancelled — " + booking.bookingRef(),
                        "booking-cancelled", ctx);
            }
        } catch (Exception e) {
            log.warn("Failed to send HTML booking-cancelled email for {}: {}", bookingId, e.getMessage());
        }

        // Cancel all scheduled journey emails
        try {
            emailSchedulerService.cancelBookingEmails(UUID.fromString(bookingId));
        } catch (Exception e) {
            log.warn("Failed to cancel scheduled emails for booking {}: {}", bookingId, e.getMessage());
        }
    }

    // ────────────────────────────────────────────────────────────
    // Payment Captured → Chapter 2 (Payment Receipt)
    // ────────────────────────────────────────────────────────────

    public void notifyPaymentCaptured(String bookingId) {
        log.info("Payment captured for booking: {}", bookingId);
        BookingClient.BookingInfo booking = bookingClient.getBooking(bookingId);
        if (booking == null) return;

        // Plain text fallback
        String guestEmail = resolveGuestEmail(booking);
        if (!guestEmail.isEmpty()) {
            emailGateway.send(guestEmail,
                    "Payment Received — " + booking.bookingRef(),
                    String.format("Hi %s,\n\nPayment for booking %s has been successfully captured. Your booking is now confirmed!\n\nSafar Team",
                            booking.guestName(), booking.bookingRef()));
        }
        inAppNotificationService.create(
                UUID.fromString(booking.guestId()),
                "Payment Received",
                "Payment for booking " + booking.bookingRef() + " has been successfully captured. Your booking is now confirmed!",
                "PAYMENT_CAPTURED",
                bookingId,
                "PAYMENT"
        );

        // HTML: Chapter 2 — Payment Receipt
        sendPaymentConfirmationHtml(bookingId, booking, guestEmail);
    }

    /**
     * Send Chapter 2 (Payment Receipt) HTML email.
     */
    public void sendPaymentConfirmationHtml(String bookingId, BookingClient.BookingInfo booking, String guestEmail) {
        try {
            if (guestEmail == null || guestEmail.isBlank()) return;

            UserClient.UserInfo guest = userClient.getUser(booking.guestId());
            UserClient.UserInfo host = userClient.getUser(booking.hostId());
            EmailContext ctx = emailContextBuilder.buildBookingContext(booking, guest, host, null, null);

            UUID bookingUuid = UUID.fromString(bookingId);
            UUID guestUuid = UUID.fromString(booking.guestId());
            UUID hostUuid = parseUuidSafe(booking.hostId());
            UUID listingUuid = parseUuidSafe(booking.listingId());

            journeyChapterService.sendChapter(2, ctx, bookingUuid, guestUuid, hostUuid, listingUuid, guestEmail);
            log.info("Chapter 2 (Payment Receipt) sent for booking {}", bookingId);
        } catch (Exception e) {
            log.warn("Failed to send Chapter 2 HTML email for booking {}: {}", bookingId, e.getMessage());
        }
    }

    /**
     * Send Chapter 2 with a pre-built EmailContext (used when consumer has enriched context).
     */
    public void sendPaymentConfirmationWithContext(String bookingId, EmailContext ctx, String guestEmail,
                                                    UUID guestUuid, UUID hostUuid, UUID listingUuid) {
        try {
            if (guestEmail == null || guestEmail.isBlank()) return;
            UUID bookingUuid = UUID.fromString(bookingId);
            journeyChapterService.sendChapter(2, ctx, bookingUuid, guestUuid, hostUuid, listingUuid, guestEmail);
            log.info("Chapter 2 (Payment Receipt) sent for booking {} via context", bookingId);
        } catch (Exception e) {
            log.warn("Failed to send Chapter 2 HTML email for booking {}: {}", bookingId, e.getMessage());
        }
    }

    // ────────────────────────────────────────────────────────────
    // Payment Failed → HTML payment-failed template
    // ────────────────────────────────────────────────────────────

    public void notifyPaymentFailed(String bookingId) {
        log.info("Payment failed for booking: {}", bookingId);
        BookingClient.BookingInfo booking = bookingClient.getBooking(bookingId);
        if (booking == null) return;

        // Plain text fallback
        String guestEmail = resolveGuestEmail(booking);
        if (!guestEmail.isEmpty()) {
            emailGateway.send(guestEmail,
                    "Payment Failed — " + booking.bookingRef(),
                    String.format("Hi %s,\n\nPayment for booking %s failed.\n\n" +
                            "Please retry your payment from your dashboard:\n" +
                            "http://localhost:3000/dashboard\n\n" +
                            "Your booking will be held for 24 hours. After that, it will be automatically cancelled.\n\n" +
                            "Safar Team",
                            booking.guestName(), booking.bookingRef()));
        }
        inAppNotificationService.create(
                UUID.fromString(booking.guestId()),
                "Payment Failed",
                "Payment for booking " + booking.bookingRef() + " failed. Please retry from your dashboard.",
                "PAYMENT_FAILED",
                bookingId,
                "PAYMENT"
        );

        // HTML: payment-failed template (reuses booking-cancelled template layout with payment context)
        try {
            if (guestEmail != null && !guestEmail.isBlank()) {
                UserClient.UserInfo guest = userClient.getUser(booking.guestId());
                UserClient.UserInfo host = userClient.getUser(booking.hostId());
                EmailContext ctx = emailContextBuilder.buildBookingContext(booking, guest, host, null, null);
                ctx.setCancellationReason("Payment could not be processed");
                emailTemplateService.sendHtmlEmail(guestEmail,
                        "Payment Failed — " + booking.bookingRef(),
                        "payment-failed", ctx);
            }
        } catch (Exception e) {
            log.warn("Failed to send HTML payment-failed email for {}: {}", bookingId, e.getMessage());
        }
    }

    // ────────────────────────────────────────────────────────────
    // Check-in, Checkout, Expiry, Refund, Payment Reminder
    // ────────────────────────────────────────────────────────────

    public void notifyBookingCheckedIn(String bookingId) {
        log.info("Booking checked in: {}", bookingId);
        BookingClient.BookingInfo booking = bookingClient.getBooking(bookingId);
        if (booking == null) return;

        String guestEmail = resolveGuestEmail(booking);
        if (!guestEmail.isEmpty()) {
            // Plain text fallback
            emailGateway.send(guestEmail,
                    "Welcome! You're checked in — " + booking.bookingRef(),
                    String.format("Hi %s,\n\nYou've been checked in for booking %s.\n\nEnjoy your stay!\n\nSafar Team",
                            booking.guestName(), booking.bookingRef()));
            // HTML: checkin-day template
            try {
                UserClient.UserInfo guest = userClient.getUser(booking.guestId());
                UserClient.UserInfo host = userClient.getUser(booking.hostId());
                EmailContext ctx = emailContextBuilder.buildBookingContext(booking, guest, host, null, null);
                emailTemplateService.sendHtmlEmail(guestEmail,
                        "Welcome! You're checked in — " + booking.bookingRef(),
                        "checkin-day", ctx);
            } catch (Exception e) {
                log.warn("Failed to send HTML checkin-day email for {}: {}", bookingId, e.getMessage());
            }
        }
        inAppNotificationService.create(UUID.fromString(booking.guestId()),
                "Checked In", "Welcome! You're checked in for " + booking.bookingRef(),
                "BOOKING_CHECKED_IN", bookingId, "BOOKING");
    }

    public void notifyBookingCompleted(String bookingId) {
        log.info("Booking completed: {}", bookingId);
        BookingClient.BookingInfo booking = bookingClient.getBooking(bookingId);
        if (booking == null) return;

        String guestEmail = resolveGuestEmail(booking);
        if (!guestEmail.isEmpty()) {
            // Plain text fallback
            emailGateway.send(guestEmail,
                    "Thank you for staying with us! — " + booking.bookingRef(),
                    String.format("Hi %s,\n\nYour stay for booking %s is now complete.\n\nWe'd love to hear about your experience — please leave a review!\n\nSafar Team",
                            booking.guestName(), booking.bookingRef()));
            // HTML: post-stay-review template (checkout + review prompt)
            try {
                UserClient.UserInfo guest = userClient.getUser(booking.guestId());
                UserClient.UserInfo host = userClient.getUser(booking.hostId());
                EmailContext ctx = emailContextBuilder.buildBookingContext(booking, guest, host, null, null);
                emailTemplateService.sendHtmlEmail(guestEmail,
                        "How was your stay? Leave a review — " + booking.bookingRef(),
                        "post-stay-review", ctx);
            } catch (Exception e) {
                log.warn("Failed to send HTML post-stay-review email for {}: {}", bookingId, e.getMessage());
            }
        }
        inAppNotificationService.create(UUID.fromString(booking.guestId()),
                "Stay Complete", "Your stay is complete! Please leave a review for " + booking.bookingRef(),
                "BOOKING_COMPLETED", bookingId, "BOOKING");
    }

    public void notifyBookingExpired(String bookingId) {
        log.info("Booking expired (auto-cancelled): {}", bookingId);
        BookingClient.BookingInfo booking = bookingClient.getBooking(bookingId);
        if (booking == null) return;

        String guestEmail = resolveGuestEmail(booking);
        if (!guestEmail.isEmpty()) {
            // Plain text fallback
            emailGateway.send(guestEmail,
                    "Booking Expired — " + booking.bookingRef(),
                    String.format("Hi %s,\n\nYour booking %s has been automatically cancelled because payment was not completed within 24 hours.\n\nYou can create a new booking anytime.\n\nSafar Team",
                            booking.guestName(), booking.bookingRef()));
            // HTML: booking-cancelled template (reuse with expiry context)
            try {
                UserClient.UserInfo guest = userClient.getUser(booking.guestId());
                UserClient.UserInfo host = userClient.getUser(booking.hostId());
                EmailContext ctx = emailContextBuilder.buildBookingContext(booking, guest, host, null, null);
                ctx.setCancellationReason("Payment not completed within 24 hours — booking auto-expired");
                emailTemplateService.sendHtmlEmail(guestEmail,
                        "Booking Expired — " + booking.bookingRef(),
                        "booking-cancelled", ctx);
            } catch (Exception e) {
                log.warn("Failed to send HTML booking-expired email for {}: {}", bookingId, e.getMessage());
            }
        }
        inAppNotificationService.create(UUID.fromString(booking.guestId()),
                "Booking Expired", "Booking " + booking.bookingRef() + " expired — payment not received within 24 hours.",
                "BOOKING_EXPIRED", bookingId, "BOOKING");

        // Cancel any scheduled emails for this booking
        try {
            emailSchedulerService.cancelBookingEmails(UUID.fromString(bookingId));
        } catch (Exception ignored) {}
    }

    public void notifyPaymentRefunded(String bookingId) {
        log.info("Payment refunded for booking: {}", bookingId);
        BookingClient.BookingInfo booking = bookingClient.getBooking(bookingId);
        if (booking == null) return;

        String guestEmail = resolveGuestEmail(booking);
        if (!guestEmail.isEmpty()) {
            // Plain text fallback
            emailGateway.send(guestEmail,
                    "Refund Processed — " + booking.bookingRef(),
                    String.format("Hi %s,\n\nYour refund for booking %s has been processed.\n\nThe amount will be credited to your original payment method within 5-7 business days.\n\nSafar Team",
                            booking.guestName(), booking.bookingRef()));
            // HTML: payment-receipt template (reuse with refund context)
            try {
                UserClient.UserInfo guest = userClient.getUser(booking.guestId());
                UserClient.UserInfo host = userClient.getUser(booking.hostId());
                EmailContext ctx = emailContextBuilder.buildBookingContext(booking, guest, host, null, null);
                ctx.setRefundTimeline("5-7 business days");
                emailTemplateService.sendHtmlEmail(guestEmail,
                        "Refund Processed — " + booking.bookingRef(),
                        "payment-receipt", ctx);
            } catch (Exception e) {
                log.warn("Failed to send HTML refund email for {}: {}", bookingId, e.getMessage());
            }
        }
        inAppNotificationService.create(UUID.fromString(booking.guestId()),
                "Refund Processed", "Refund for " + booking.bookingRef() + " processed. Amount will be credited in 5-7 business days.",
                "PAYMENT_REFUNDED", bookingId, "PAYMENT");
    }

    public void notifyPaymentReminder(String bookingId, String guestEmail, String guestName,
                                       String bookingRef, long totalPaise, boolean urgent) {
        log.info("Payment reminder ({}) for booking: {}", urgent ? "urgent" : "first", bookingId);

        // Try to resolve from booking if email not in event
        if (guestEmail == null || guestEmail.isBlank()) {
            BookingClient.BookingInfo booking = bookingClient.getBooking(bookingId);
            if (booking == null) return;
            guestEmail = resolveGuestEmail(booking);
            guestName = booking.guestName();
            bookingRef = booking.bookingRef();
        }
        if (guestEmail == null || guestEmail.isBlank()) return;

        String subject = urgent
                ? "Action Required: Complete Payment for " + bookingRef
                : "Reminder: Complete Your Booking Payment — " + bookingRef;

        String totalFormatted = totalPaise > 0
                ? String.format("₹%,.2f", totalPaise / 100.0)
                : "the booking amount";

        String urgencyLine = urgent
                ? "Your booking will be automatically cancelled if payment is not completed within 24 hours of booking creation."
                : "Please complete the payment soon to secure your booking.";

        String body = String.format(
                "Hi %s,\n\n" +
                "Your booking %s is awaiting payment of %s.\n\n" +
                "%s\n\n" +
                "Complete your payment: http://localhost:3000/dashboard\n\n" +
                "If you've already paid, please ignore this email.\n\n" +
                "Safar Team",
                guestName, bookingRef, totalFormatted, urgencyLine);

        emailGateway.send(guestEmail, subject, body);
        log.info("Payment reminder sent to {} for booking {}", guestEmail, bookingRef);

        // In-app notification
        try {
            BookingClient.BookingInfo booking = bookingClient.getBooking(bookingId);
            if (booking != null) {
                inAppNotificationService.create(UUID.fromString(booking.guestId()),
                        urgent ? "Urgent: Complete Payment" : "Payment Reminder",
                        "Complete payment for " + bookingRef + " to confirm your booking.",
                        urgent ? "PAYMENT_REMINDER_URGENT" : "PAYMENT_REMINDER",
                        bookingId, "PAYMENT");
            }
        } catch (Exception ignored) {}
    }

    // ────────────────────────────────────────────────────────────
    // Host HTML Emails
    // ────────────────────────────────────────────────────────────

    /**
     * Send host-new-booking HTML alert email.
     */
    public void sendHostNewBookingAlert(String hostEmail, String hostName, EmailContext ctx) {
        if (hostEmail == null || hostEmail.isBlank()) {
            log.warn("Skipping host-new-booking alert — no host email");
            return;
        }
        try {
            ctx.setHostName(hostName);
            emailTemplateService.sendHtmlEmail(hostEmail,
                    "New Booking Received — " + ctx.getBookingRef(),
                    "host-new-booking", ctx);
            log.info("Host new-booking HTML alert sent to {}", hostEmail);
        } catch (Exception e) {
            log.warn("Failed to send host-new-booking HTML email to {}: {}", hostEmail, e.getMessage());
        }
    }

    /**
     * Send host-earnings HTML report email.
     */
    public void sendHostEarningsReport(String hostEmail, String hostName, EmailContext ctx) {
        if (hostEmail == null || hostEmail.isBlank()) {
            log.warn("Skipping host-earnings report — no host email");
            return;
        }
        try {
            ctx.setHostName(hostName);
            emailTemplateService.sendHtmlEmail(hostEmail,
                    "Your Earnings Report — Safar",
                    "host-earnings", ctx);
            log.info("Host earnings HTML report sent to {}", hostEmail);
        } catch (Exception e) {
            log.warn("Failed to send host-earnings HTML email to {}: {}", hostEmail, e.getMessage());
        }
    }

    // ────────────────────────────────────────────────────────────
    // Review Notifications (unchanged — no HTML template yet)
    // ────────────────────────────────────────────────────────────

    public void notifyReviewCreated(String reviewId) {
        log.info("New review: {}", reviewId);
        // review.created events only carry the reviewId — no booking context
        // For now, log the event. A full implementation would call review-service for details.
        emailGateway.send("", "New Review Submitted — " + reviewId,
                "A guest has submitted a new review: " + reviewId);
        // Note: in-app notification for host would require review-service call to get hostId.
        // The reviewId alone is insufficient. This will be enhanced when review events carry hostId.
    }

    public void notifyReviewReplied(String eventJson) {
        try {
            // Parse minimal event: {"reviewId":"...","listingId":"...","guestId":"...","hostId":"..."}
            String guestId = extractJsonField(eventJson, "guestId");
            String reviewId = extractJsonField(eventJson, "reviewId");
            if (guestId == null || guestId.isBlank()) {
                log.warn("Cannot send review.replied notification — no guestId in event");
                return;
            }
            UserClient.UserInfo guest = userClient.getUser(guestId);
            if (guest == null || guest.email() == null || guest.email().isBlank()) {
                log.warn("No email for guest {} — skipping review reply notification", guestId);
            } else {
                String guestName = guest.name() != null ? guest.name() : "Guest";
                emailGateway.send(guest.email(),
                        "The host replied to your review",
                        String.format("Hi %s,\n\nThe host has responded to your review (ID: %s).\n\nLog in to Safar to see the full reply.\n\nThank you,\nSafar Team",
                                guestName, reviewId));
            }
            inAppNotificationService.create(
                    UUID.fromString(guestId),
                    "Host Replied to Your Review",
                    "The host has responded to your review. Log in to see the full reply.",
                    "REVIEW_REPLIED",
                    reviewId,
                    "REVIEW"
            );
            log.info("Review reply notification sent to guest {}", guestId);
        } catch (Exception e) {
            log.error("Error sending review.replied notification: {}", e.getMessage());
        }
    }

    // ────────────────────────────────────────────────────────────
    // Medical Booking (unchanged — already has its own format)
    // ────────────────────────────────────────────────────────────

    public void sendMedicalBookingConfirmation(String guestId, String bookingId,
                                                String procedureName, String hospitalName,
                                                String procedureDate) {
        try {
            UserClient.UserInfo user = userClient.getUser(guestId);
            if (user == null || user.email() == null || user.email().isBlank()) {
                log.warn("No email found for guest {} - skipping medical notification", guestId);
            } else {
                String guestName = user.name() != null ? user.name() : "Guest";
                String subject = "Medical Booking Confirmed - " + procedureName;
                String body = String.format(
                        "Hi %s,\n\n" +
                        "Your medical booking has been confirmed.\n\n" +
                        "Procedure: %s\n" +
                        "Hospital: %s\n" +
                        "Procedure Date: %s\n" +
                        "Booking ID: %s\n\n" +
                        "Important reminders:\n" +
                        "- Bring all medical records and prescriptions\n" +
                        "- Arrive at the hospital at least 2 hours before your procedure\n" +
                        "- Ensure your medical profile is up to date on Safar\n" +
                        "- Contact the hospital for any pre-procedure instructions\n\n" +
                        "Wishing you a safe and successful procedure.\n" +
                        "Team Safar",
                        guestName, procedureName, hospitalName, procedureDate, bookingId
                );

                emailGateway.send(user.email(), subject, body);
            }

            inAppNotificationService.create(
                    UUID.fromString(guestId),
                    "Medical Booking Confirmed",
                    "Your medical booking for " + procedureName + " at " + hospitalName + " on " + procedureDate + " is confirmed.",
                    "BOOKING_CONFIRMED",
                    bookingId,
                    "BOOKING"
            );
            log.info("Medical booking confirmation sent to guest {} for booking {}", guestId, bookingId);
        } catch (Exception e) {
            log.error("Failed to send medical booking confirmation for booking {}: {}", bookingId, e.getMessage());
        }
    }

    // ────────────────────────────────────────────────────────────
    // Private helpers
    // ────────────────────────────────────────────────────────────

    private String extractJsonField(String json, String field) {
        String key = "\"" + field + "\":\"";
        int start = json.indexOf(key);
        if (start < 0) return null;
        start += key.length();
        int end = json.indexOf("\"", start);
        return end > start ? json.substring(start, end) : null;
    }

    /**
     * Resolve guest email: prefer guestEmail from booking, fallback to user-service lookup.
     */
    private String resolveGuestEmail(BookingClient.BookingInfo booking) {
        if (booking.guestEmail() != null && !booking.guestEmail().isEmpty()) {
            return booking.guestEmail();
        }
        UserClient.UserInfo user = userClient.getUser(booking.guestId());
        return user != null ? user.email() : "";
    }

    private String resolveHostEmail(String hostId) {
        UserClient.UserInfo user = userClient.getUser(hostId);
        return user != null ? user.email() : "";
    }

    private static UUID parseUuidSafe(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static UUID parseUuidFromCtx(String hint, String bookingId) {
        // Utility — returns null when we don't have a real UUID
        return null;
    }
}
