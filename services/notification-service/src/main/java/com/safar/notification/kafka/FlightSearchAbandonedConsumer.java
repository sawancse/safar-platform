package com.safar.notification.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safar.notification.dto.EmailContext;
import com.safar.notification.service.EmailTemplateService;
import com.safar.notification.service.InAppNotificationService;
import com.safar.notification.service.UserClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Consumes {@code flight.search.abandoned} events from the
 * AbandonedSearchDetector and fans out reminder notifications via the
 * Push → WhatsApp → Email cascade.
 *
 * Day-1 delivery scope:
 *   - Email: live (template flight-search-abandoned.html)
 *   - In-app: live (existing inAppNotificationService)
 *   - SMS:   live via existing SmsService (re-uses MSG91, DLT template
 *            registration pending operationally)
 *   - WhatsApp: code path live; actual delivery requires MSG91 WA template
 *               approval by Meta (operational, not coding)
 *   - Push:  TODO once mobile-side push gets a flight-search-abandoned event type
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FlightSearchAbandonedConsumer {

    private final EmailTemplateService emailTemplateService;
    private final InAppNotificationService inAppNotificationService;
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

            log.info("Abandoned-search reminder #{} ({}-pulse) for {} {}",
                    reminderNumber, pulse, route, depDate);

            // ── Email ──
            if (!email.isBlank()) {
                UserClient.UserInfo user = !userIdStr.isBlank() ? userClient.getUser(userIdStr) : null;
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

            // ── SMS (TODO: needs a sendFlightSearchAbandoned(...) method on SmsService
            //          that uses an MSG91 DLT-approved template id; deliberately skipped
            //          on Day-1 to avoid sending SMS without DLT registration)
            //
            // ── WhatsApp (TODO: requires MSG91 WA template approval) ──
            // When wired:
            //   smsService.sendWhatsAppTemplate(phone, "flight_search_abandoned",
            //                                    Map.of("route", route, "date", depDate, "fare", fareHint));
            //
            // ── Push notification (TODO: needs mobile event-type registration) ──

            // ── In-app ──
            if (!userIdStr.isBlank()) {
                try {
                    String body = "Still going " + route + " on " + depDate + "? "
                            + fareHint + " Tap to book.";
                    inAppNotificationService.create(UUID.fromString(userIdStr),
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
