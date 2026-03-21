package com.safar.user.controller;

import com.safar.user.service.HostSubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    @Value("${razorpay.webhook-secret:webhook_secret_placeholder}")
    private String webhookSecret;

    private final HostSubscriptionService hostSubscriptionService;

    @PostMapping("/razorpay")
    public ResponseEntity<Void> handleRazorpay(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {

        if (signature != null && !verifySignature(rawBody, signature)) {
            log.warn("Invalid Razorpay webhook signature");
            return ResponseEntity.badRequest().build();
        }

        try {
            JSONObject payload = new JSONObject(rawBody);
            String event = payload.getString("event");
            JSONObject entity = payload.getJSONObject("payload")
                    .getJSONObject("subscription")
                    .getJSONObject("entity");
            String razorpaySubId = entity.getString("id");

            log.info("Razorpay webhook: event={} subId={}", event, razorpaySubId);

            switch (event) {
                case "subscription.activated" ->
                        hostSubscriptionService.onSubscriptionActivated(razorpaySubId, entity);
                case "subscription.charged" ->
                        hostSubscriptionService.onSubscriptionCharged(razorpaySubId, entity);
                case "subscription.cancelled", "subscription.completed" ->
                        hostSubscriptionService.onSubscriptionCancelled(razorpaySubId);
                case "subscription.pending" ->
                        hostSubscriptionService.onSubscriptionPending(razorpaySubId);
                default -> log.debug("Unhandled webhook event: {}", event);
            }
        } catch (Exception e) {
            log.error("Error processing Razorpay webhook", e);
        }

        return ResponseEntity.ok().build();
    }

    private boolean verifySignature(String payload, String signature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computed = HexFormat.of().formatHex(hash);
            return MessageDigest.isEqual(computed.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Signature verification failed", e);
            return false;
        }
    }
}
