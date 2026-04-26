package com.safar.notification.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safar.notification.dto.EmailContext;
import com.safar.notification.service.EmailTemplateService;
import com.safar.notification.service.InAppNotificationService;
import com.safar.notification.service.PushNotificationService;
import com.safar.notification.service.SmsService;
import com.safar.notification.service.UserClient;
import com.safar.notification.service.WhatsAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Consumes {@code flight.search.abandoned} events from the
 * AbandonedSearchDetector and fans out reminders across the full
 * Push → WhatsApp → Email + SMS + In-App cascade.
 *
 * Channel-per-pulse strategy:
 *   1H pulse  — Push + WhatsApp + Email + In-app  (high frequency, low friction)
 *   6H pulse  — Push + WhatsApp + Email + In-app  (same as 1H)
 *   24H pulse — Push + WhatsApp + Email + SMS + In-app  (last chance, add SMS)
 *
 * Channel-by-channel "live" status:
 *   Email      — LIVE (template flight-search-abandoned.html)
 *   In-app     — LIVE (existing inAppNotificationService)
 *   SMS        — LIVE in code; activates when MSG91 DLT template id is set
 *                in env (msg91.sms.flight-search-abandoned-template-id)
 *   WhatsApp   — LIVE in code; activates when Meta-approved template name
 *                is set in env (msg91.wa.flight-search-abandoned-template)
 *   Push       — LIVE in code; activates when mobile starts capturing
 *                Expo push tokens + user-service exposes /push-tokens lookup
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FlightSearchAbandonedConsumer {

    private final EmailTemplateService emailTemplateService;
    private final InAppNotificationService inAppNotificationService;
    private final SmsService smsService;
    private final WhatsAppService whatsAppService;
    private final PushNotificationService pushNotificationService;
    private final UserClient userClient;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "flight.search.abandoned", groupId = "notification-flight-abandoned-group")
    public void onAbandoned(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);

            String userIdStr = node.path("userId").asText("");
            String email = node.path("contactEmail").asText("");
            String phone = node.path("contactPhone").asText("");
            String origin = node.path("origin").asText("");
            String destination = node.path("destination").asText("");
            String depDate = node.path("departureDate").asText("");
            int reminderNumber = node.path("reminderNumber").asInt(1);
            String pulse = node.path("pulse").asText("1H");
            long farePaise = node.path("cheapestFarePaiseAtSearch").asLong(0);
            String fareTrend = node.path("fareTrend").asText("STABLE");

            String route = origin + " → " + destination;
            String fareHint = formatFareHint(fareTrend, farePaise);
            UUID userId = parseUuid(userIdStr);

            log.info("Abandoned-search reminder #{} ({}-pulse) for {} {} via Push+WA+Email{}",
                    reminderNumber, pulse, route, depDate,
                    "24H".equals(pulse) ? "+SMS" : "");

            // ── Push (every pulse, mobile users only) ──
            if (userId != null) {
                try {
                    pushNotificationService.sendFlightSearchAbandoned(userId, route, depDate, fareHint);
                } catch (Exception e) {
                    log.warn("Push reminder failed: {}", e.getMessage());
                }
            }

            // ── WhatsApp (every pulse, highest engagement) ──
            if (!phone.isBlank()) {
                try {
                    whatsAppService.sendFlightSearchAbandoned(phone, route, depDate, fareHint);
                } catch (Exception e) {
                    log.warn("WhatsApp reminder failed: {}", e.getMessage());
                }
            }

            // ── Email (every pulse) ──
            if (!email.isBlank()) {
                UserClient.UserInfo user = userId != null ? userClient.getUser(userIdStr) : null;
                String name = user != null ? user.name() : "Traveller";

                EmailContext ctx = new EmailContext();
                ctx.setGuestName(name);
                ctx.setFlightRoute(route);
                ctx.setFlightDate(depDate);
                ctx.setTotalAmount(fareHint);

                emailTemplateService.sendHtmlEmail(email,
                        subjectFor(reminderNumber, route, depDate, fareTrend),
                        "flight-search-abandoned", ctx);
                log.info("Sent abandoned-search reminder email to {} (reminder #{})", email, reminderNumber);
            }

            // ── SMS (24H pulse only — last chance, most expensive channel) ──
            if (!phone.isBlank() && "24H".equals(pulse)) {
                try {
                    smsService.sendFlightSearchAbandoned(phone, route, depDate, fareHint);
                } catch (Exception e) {
                    log.warn("SMS reminder failed: {}", e.getMessage());
                }
            }

            // ── In-app (every pulse, logged-in users only) ──
            if (userId != null) {
                try {
                    String body = "Still going " + route + " on " + depDate + "? "
                            + fareHint + " Tap to book.";
                    inAppNotificationService.create(userId,
                            "Complete your flight search",
                            body,
                            "FLIGHT_SEARCH_ABANDONED",
                            node.path("eventId").asText(""),
                            "FLIGHT_SEARCH");
                } catch (Exception e) {
                    log.warn("In-app reminder failed: {}", e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Failed to handle abandoned-search event: {}", e.getMessage(), e);
        }
    }

    private static UUID parseUuid(String s) {
        if (s == null || s.isBlank()) return null;
        try { return UUID.fromString(s); } catch (Exception e) { return null; }
    }

    private static String subjectFor(int reminderNumber, String route, String depDate, String fareTrend) {
        return switch (reminderNumber) {
            case 1 -> "Still going " + route + "? Your flight is waiting";
            case 2 -> "Flights on " + route + " for " + depDate + " — book before they fill up";
            default -> "Last reminder: " + route + " flights for " + depDate;
        };
    }

    private static String formatFareHint(String fareTrend, long farePaise) {
        if (farePaise <= 0) return "";
        String price = "₹" + String.format("%,d", farePaise / 100);
        return switch (fareTrend) {
            case "DROPPED" -> "Fares dropped! From " + price + " now.";
            case "RISING" -> "Fares rising — book before they go up. From " + price + ".";
            default -> "Fares stable around " + price + ".";
        };
    }
}
