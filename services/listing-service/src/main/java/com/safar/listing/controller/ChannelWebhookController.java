package com.safar.listing.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/webhooks/channex")
@RequiredArgsConstructor
@Slf4j
public class ChannelWebhookController {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Receive Channex.io webhook notifications.
     * Events: new_booking, booking_cancelled, rate_update_confirmed, availability_update_confirmed
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> handleWebhook(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "X-Channex-Signature", required = false) String signature) {

        String eventType = (String) payload.getOrDefault("event", "unknown");
        log.info("Channex webhook received: type={}", eventType);

        switch (eventType) {
            case "new_booking" -> {
                Map<String, Object> booking = (Map<String, Object>) payload.get("data");
                if (booking != null) {
                    kafkaTemplate.send("channel.booking.received",
                            booking.getOrDefault("id", "").toString(), payload);
                    log.info("New booking from channel: {}", booking.get("channel"));
                }
            }
            case "booking_cancelled" -> {
                Map<String, Object> booking = (Map<String, Object>) payload.get("data");
                if (booking != null) {
                    kafkaTemplate.send("channel.booking.cancelled",
                            booking.getOrDefault("id", "").toString(), payload);
                    log.info("Booking cancelled from channel");
                }
            }
            case "rate_update_confirmed" -> log.info("Rate update confirmed by channel");
            case "availability_update_confirmed" -> log.info("Availability update confirmed by channel");
            default -> log.warn("Unknown Channex webhook event: {}", eventType);
        }

        return ResponseEntity.ok(Map.of("status", "received"));
    }
}
