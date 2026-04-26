package com.safar.notification.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safar.notification.service.WhatsAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;

/**
 * Inbound WhatsApp webhook for "Book on WhatsApp" — MSG91 forwards user
 * messages to this endpoint, we parse the keyword and reply via
 * WhatsAppService with a deep link back to safar-web.
 *
 * Day-1 scope: keyword-based bot (BOOK FLIGHT, BOOK INSURANCE, STATUS,
 * HELP). Phase-2: extend to multi-step WhatsApp Flows for in-WA booking.
 *
 * Signature verification mirrors the Duffel webhook pattern. MSG91
 * signs the payload with HMAC-SHA256 using the webhook secret;
 * we verify constant-time.
 */
@RestController
@RequestMapping("/api/v1/whatsapp/webhook")
@RequiredArgsConstructor
@Slf4j
public class WhatsAppWebhookController {

    private static final String SIG_HEADER = "X-MSG91-Signature";

    private final WhatsAppService whatsAppService;
    private final ObjectMapper objectMapper;

    @Value("${msg91.wa.webhook-secret:}")
    private String webhookSecret;

    @Value("${safar.web.base-url:https://ysafar.com}")
    private String webBaseUrl;

    @PostMapping
    public ResponseEntity<Map<String, Object>> receive(
            @RequestHeader(value = SIG_HEADER, required = false) String signature,
            @RequestBody String rawBody) {

        if (!verifySignature(signature, rawBody)) {
            log.warn("WhatsApp webhook signature mismatch — rejecting");
            return ResponseEntity.status(401).body(Map.of("ok", false, "reason", "invalid signature"));
        }

        try {
            JsonNode root = objectMapper.readTree(rawBody);
            // MSG91 inbound payload (rough shape; varies by API version):
            // {
            //   "type": "message",
            //   "message": { "from": "919876543210", "text": "BOOK FLIGHT", "id": "...", "timestamp": "..." }
            // }
            JsonNode message = root.path("message");
            String from = message.path("from").asText("");
            String text = message.path("text").asText("").trim().toUpperCase();

            if (from.isBlank() || text.isBlank()) {
                log.debug("WA inbound missing from/text — ignoring");
                return ResponseEntity.ok(Map.of("ok", true, "matched", false));
            }

            log.info("WA inbound from {}: '{}'", maskPhone(from), text);
            String intent = matchIntent(text);
            handle(intent, from);

            return ResponseEntity.ok(Map.of("ok", true, "intent", intent));
        } catch (Exception e) {
            log.error("WA webhook handling error: {}", e.getMessage(), e);
            // Return 200 so MSG91 doesn't retry-storm on a bug
            return ResponseEntity.ok(Map.of("ok", false, "reason", e.getMessage()));
        }
    }

    private static String matchIntent(String text) {
        if (text.contains("BOOK") && text.contains("FLIGHT")) return "BOOK_FLIGHT";
        if (text.contains("BOOK") && text.contains("INSURANCE")) return "BOOK_INSURANCE";
        if (text.contains("STATUS") || text.contains("MY BOOKING")) return "STATUS";
        if (text.contains("HELP") || text.contains("HI") || text.equals("HELLO")) return "HELP";
        return "UNKNOWN";
    }

    private void handle(String intent, String from) {
        switch (intent) {
            case "BOOK_FLIGHT" -> whatsAppService.sendBookFlightDeeplink(from,
                    webBaseUrl + "/flights?utm_source=whatsapp&utm_medium=bot");
            case "BOOK_INSURANCE" -> whatsAppService.sendBookInsuranceDeeplink(from,
                    webBaseUrl + "/insurance?utm_source=whatsapp&utm_medium=bot");
            case "STATUS" -> whatsAppService.sendBookFlightDeeplink(from,
                    webBaseUrl + "/dashboard?utm_source=whatsapp&utm_medium=bot");
            case "HELP", "UNKNOWN" -> whatsAppService.sendBotHelp(from);
            default -> log.info("Unhandled intent {} from {}", intent, maskPhone(from));
        }
    }

    private boolean verifySignature(String header, String body) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.warn("msg91.wa.webhook-secret not set; accepting WA webhook unsigned (dev only)");
            return true;
        }
        if (header == null || header.isBlank()) return false;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String computed = HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
            return constantTimeEquals(header, computed);
        } catch (Exception e) {
            log.error("WA HMAC verification error: {}", e.getMessage());
            return false;
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) return false;
        int diff = 0;
        for (int i = 0; i < a.length(); i++) diff |= a.charAt(i) ^ b.charAt(i);
        return diff == 0;
    }

    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "***";
        return "***" + phone.substring(phone.length() - 4);
    }
}
