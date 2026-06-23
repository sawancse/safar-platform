package com.safar.insurance.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

/**
 * Razorpay order-create + signature/webhook verification for insurance premium collection.
 * Gated by {@code razorpay.enabled} + keys — when off, the marketplace falls back to
 * sandbox instant-issue (no charge). Self-contained (does not depend on payment-service)
 * so insurance-service can collect premium directly under the POSP "you-collect" model.
 */
@Service
@Slf4j
public class RazorpayService {

    private final WebClient razorpayWebClient;
    private final ObjectMapper objectMapper;

    @Value("${razorpay.enabled:false}")
    private boolean enabled;

    @Value("${razorpay.key-id:}")
    private String keyId;

    @Value("${razorpay.key-secret:}")
    private String keySecret;

    @Value("${razorpay.webhook-secret:}")
    private String webhookSecret;

    public RazorpayService(@Qualifier("razorpayWebClient") WebClient razorpayWebClient, ObjectMapper objectMapper) {
        this.razorpayWebClient = razorpayWebClient;
        this.objectMapper = objectMapper;
    }

    public boolean isEnabled() {
        return enabled && keyId != null && !keyId.isBlank() && keySecret != null && !keySecret.isBlank();
    }

    public String getKeyId() { return keyId; }

    /** Create a Razorpay order; returns the order id (e.g. order_XXXX). */
    public String createOrder(long amountPaise, String receipt) {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("amount", amountPaise);
            body.put("currency", "INR");
            body.put("receipt", receipt);
            body.put("payment_capture", 1);
            String basic = Base64.getEncoder().encodeToString((keyId + ":" + keySecret).getBytes(StandardCharsets.UTF_8));
            String raw = razorpayWebClient.post()
                    .uri("/v1/orders")
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + basic)
                    .bodyValue(objectMapper.writeValueAsString(body))
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();
            JsonNode resp = objectMapper.readTree(raw == null ? "{}" : raw);
            String orderId = resp.path("id").asText("");
            if (orderId.isBlank()) throw new IllegalStateException("Razorpay returned no order id: " + raw);
            return orderId;
        } catch (Exception e) {
            throw new IllegalStateException("Razorpay order creation failed: " + e.getMessage(), e);
        }
    }

    /** Verify checkout signature: HMAC_SHA256(orderId|paymentId, keySecret) == signature. */
    public boolean verifyPaymentSignature(String orderId, String paymentId, String signature) {
        if (orderId == null || paymentId == null || signature == null) return false;
        return hmacSha256Hex(orderId + "|" + paymentId, keySecret).equalsIgnoreCase(signature);
    }

    /** Verify webhook payload signature using the webhook secret. */
    public boolean verifyWebhookSignature(String rawPayload, String signature) {
        if (webhookSecret == null || webhookSecret.isBlank() || rawPayload == null || signature == null) return false;
        return hmacSha256Hex(rawPayload, webhookSecret).equalsIgnoreCase(signature);
    }

    private static String hmacSha256Hex(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] out = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(out.length * 2);
            for (byte b : out) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("HMAC error: " + e.getMessage(), e);
        }
    }
}
