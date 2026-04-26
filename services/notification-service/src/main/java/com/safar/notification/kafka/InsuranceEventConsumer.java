package com.safar.notification.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safar.notification.service.InAppNotificationService;
import com.safar.notification.service.WhatsAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Consumes insurance policy lifecycle events. Day-1 scope: WhatsApp +
 * In-app on policy issued / cancelled. Email template can be added
 * later (current MVP relies on the provider's own cert email).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InsuranceEventConsumer {

    private final WhatsAppService whatsAppService;
    private final InAppNotificationService inAppNotificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = {"insurance.policy.issued", "insurance.policy.cancelled"},
            groupId = "notification-insurance-group"
    )
    public void onInsuranceEvent(String message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        try {
            JsonNode node = objectMapper.readTree(message);
            switch (topic) {
                case "insurance.policy.issued" -> handleIssued(node);
                case "insurance.policy.cancelled" -> handleCancelled(node);
                default -> log.warn("Unhandled insurance topic: {}", topic);
            }
        } catch (Exception e) {
            log.error("Insurance event handling error on {}: {}", topic, e.getMessage(), e);
        }
    }

    private void handleIssued(JsonNode node) {
        String userId = node.path("userId").asText("");
        String phone = node.path("contactPhone").asText("");
        String policyRef = node.path("policyRef").asText("");
        String provider = node.path("provider").asText("");
        String coverage = node.path("coverageType").asText("");
        String certUrl = node.path("certificateUrl").asText("");

        if (!phone.isBlank()) {
            try {
                whatsAppService.sendInsurancePolicyIssued(phone, policyRef, provider, coverage, certUrl);
            } catch (Exception e) {
                log.warn("WA insurance-issued failed: {}", e.getMessage());
            }
        }

        if (!userId.isBlank()) {
            try {
                inAppNotificationService.create(UUID.fromString(userId),
                        "Insurance issued",
                        "Your travel insurance " + policyRef + " is active. Tap to view your certificate.",
                        "INSURANCE_ISSUED", node.path("policyId").asText(""), "INSURANCE_POLICY");
            } catch (Exception e) {
                log.warn("In-app insurance-issued failed: {}", e.getMessage());
            }
        }
    }

    private void handleCancelled(JsonNode node) {
        String userId = node.path("userId").asText("");
        String policyRef = node.path("policyRef").asText("");

        if (!userId.isBlank()) {
            try {
                inAppNotificationService.create(UUID.fromString(userId),
                        "Insurance cancelled",
                        "Your travel insurance " + policyRef + " has been cancelled. Refund (if eligible) will be processed in 5-7 days.",
                        "INSURANCE_CANCELLED", node.path("policyId").asText(""), "INSURANCE_POLICY");
            } catch (Exception e) {
                log.warn("In-app insurance-cancelled failed: {}", e.getMessage());
            }
        }
    }
}
