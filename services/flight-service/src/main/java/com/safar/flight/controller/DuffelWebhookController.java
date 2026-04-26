package com.safar.flight.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safar.flight.entity.FlightBooking;
import com.safar.flight.repository.FlightBookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Duffel webhook receiver.
 *
 * Configure in Duffel dashboard: POST {public-base}/api/v1/flights/webhooks/duffel.
 * Duffel signs the body with HMAC-SHA256 using your webhook secret; we verify
 * via the {@code X-Duffel-Signature} header (format {@code sha256=<hex>}).
 *
 * Handled event types:
 * - order.airline_initiated_change — airline rescheduled or cancelled the flight
 * - order.cancelled — order was cancelled (locally or by airline)
 *
 * Each event is forwarded to Kafka so notification-service can email/SMS the user.
 *
 * IMPORTANT: this endpoint must be added to the api-gateway public-paths list.
 */
@RestController
@RequestMapping("/api/v1/flights/webhooks")
@RequiredArgsConstructor
@Slf4j
public class DuffelWebhookController {

    private static final String SIG_HEADER = "X-Duffel-Signature";
    private static final String SIG_PREFIX = "sha256=";

    private final FlightBookingRepository bookingRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${duffel.webhook-secret:}")
    private String webhookSecret;

    @PostMapping("/duffel")
    public ResponseEntity<Map<String, Object>> receive(
            @RequestHeader(value = SIG_HEADER, required = false) String signatureHeader,
            @RequestBody String rawBody
    ) {
        if (!verifySignature(signatureHeader, rawBody)) {
            log.warn("Duffel webhook signature mismatch — rejecting");
            return ResponseEntity.status(401).body(Map.of("ok", false, "reason", "invalid signature"));
        }

        try {
            JsonNode root = objectMapper.readTree(rawBody);
            String eventType = root.path("type").asText("");
            JsonNode object = root.path("data").path("object");
            String externalOrderId = object.path("id").asText("");
            log.info("Duffel webhook received: type={} order={}", eventType, externalOrderId);

            Optional<FlightBooking> bookingOpt = bookingRepository.findByDuffelOrderId(externalOrderId);
            if (bookingOpt.isEmpty()) {
                log.warn("Duffel webhook for unknown order {}; ignoring", externalOrderId);
                return ResponseEntity.ok(Map.of("ok", true, "matched", false));
            }
            FlightBooking booking = bookingOpt.get();

            switch (eventType) {
                case "order.airline_initiated_change" -> publishChangeEvent(booking, object);
                case "order.cancelled" -> publishCancelEvent(booking);
                default -> log.info("Duffel event type {} acknowledged but not handled", eventType);
            }

            return ResponseEntity.ok(Map.of("ok", true, "matched", true, "bookingRef", booking.getBookingRef()));
        } catch (Exception e) {
            log.error("Failed to process Duffel webhook", e);
            // Return 200 anyway so Duffel doesn't endlessly retry on a bug; we logged it.
            return ResponseEntity.ok(Map.of("ok", false, "reason", e.getMessage()));
        }
    }

    private void publishChangeEvent(FlightBooking booking, JsonNode object) {
        try {
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("bookingId", booking.getId().toString());
            event.put("bookingRef", booking.getBookingRef());
            event.put("userId", booking.getUserId().toString());
            event.put("externalOrderId", booking.getDuffelOrderId());
            event.put("airline", Optional.ofNullable(booking.getAirline()).orElse(""));
            event.put("flightNumber", Optional.ofNullable(booking.getFlightNumber()).orElse(""));
            event.put("departureCityCode", Optional.ofNullable(booking.getDepartureCityCode()).orElse(""));
            event.put("arrivalCityCode", Optional.ofNullable(booking.getArrivalCityCode()).orElse(""));
            event.put("contactEmail", Optional.ofNullable(booking.getContactEmail()).orElse(""));
            event.put("contactPhone", Optional.ofNullable(booking.getContactPhone()).orElse(""));
            event.put("changeReason", object.path("change_reason").asText("Schedule change"));
            event.put("rawObject", object.toString());

            kafkaTemplate.send("flight.airline.changed",
                    booking.getId().toString(), objectMapper.writeValueAsString(event));
            log.info("Published flight.airline.changed for {}", booking.getBookingRef());
        } catch (Exception e) {
            log.error("Failed to publish flight.airline.changed for {}", booking.getBookingRef(), e);
        }
    }

    private void publishCancelEvent(FlightBooking booking) {
        try {
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("bookingId", booking.getId().toString());
            event.put("bookingRef", booking.getBookingRef());
            event.put("userId", booking.getUserId().toString());
            event.put("source", "DUFFEL_WEBHOOK");
            kafkaTemplate.send("flight.booking.cancelled",
                    booking.getId().toString(), objectMapper.writeValueAsString(event));
        } catch (Exception e) {
            log.error("Failed to publish flight.booking.cancelled webhook event", e);
        }
    }

    private boolean verifySignature(String header, String body) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            // No secret configured: accept (e.g. local dev). Production deployments
            // MUST set DUFFEL_WEBHOOK_SECRET env var.
            log.warn("duffel.webhook-secret not set; accepting webhook without verification");
            return true;
        }
        if (header == null || !header.startsWith(SIG_PREFIX)) {
            return false;
        }
        String provided = header.substring(SIG_PREFIX.length());
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            String computed = HexFormat.of().formatHex(hash);
            return constantTimeEquals(provided, computed);
        } catch (Exception e) {
            log.error("HMAC verification error: {}", e.getMessage());
            return false;
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) return false;
        int diff = 0;
        for (int i = 0; i < a.length(); i++) diff |= a.charAt(i) ^ b.charAt(i);
        return diff == 0;
    }
}
