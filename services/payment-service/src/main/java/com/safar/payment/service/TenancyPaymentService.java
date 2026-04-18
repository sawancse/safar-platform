package com.safar.payment.service;

import com.safar.payment.entity.TenancySubscription;
import com.safar.payment.gateway.PaymentGateway;
import com.safar.payment.gateway.SubscriptionResult;
import com.safar.payment.repository.TenancySubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class TenancyPaymentService {

    private final TenancySubscriptionRepository subscriptionRepository;
    private final PaymentGateway paymentGateway;
    private final RazorpayGateway razorpayGateway;
    private final WebhookService webhookService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String webhookSecret;

    public TenancyPaymentService(
            TenancySubscriptionRepository subscriptionRepository,
            @Qualifier("razorpayGateway") PaymentGateway paymentGateway,
            RazorpayGateway razorpayGateway,
            WebhookService webhookService,
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${razorpay.webhook-secret:#{null}}") String webhookSecret) {
        this.subscriptionRepository = subscriptionRepository;
        this.paymentGateway = paymentGateway;
        this.razorpayGateway = razorpayGateway;
        this.webhookService = webhookService;
        this.kafkaTemplate = kafkaTemplate;
        this.webhookSecret = webhookSecret;
    }

    /** Thin return carrier so the controller can surface Razorpay's short_url alongside the persisted row. */
    public record CreateSubResult(TenancySubscription subscription, String shortUrl) {}

    @Transactional
    public TenancySubscription createRentSubscription(UUID tenancyId, UUID tenantId,
                                                       long monthlyAmountPaise, String tenancyRef) {
        return createRentSubscriptionWithUrl(tenancyId, tenantId, monthlyAmountPaise, tenancyRef).subscription();
    }

    @Transactional
    public CreateSubResult createRentSubscriptionWithUrl(UUID tenancyId, UUID tenantId,
                                                          long monthlyAmountPaise, String tenancyRef) {
        // Check if subscription already exists
        if (subscriptionRepository.findByTenancyId(tenancyId).isPresent()) {
            throw new RuntimeException("Subscription already exists for tenancy: " + tenancyId);
        }

        SubscriptionResult result = paymentGateway.createSubscription(
                "PG-Rent-" + tenancyRef, monthlyAmountPaise, "INR");

        TenancySubscription subscription = TenancySubscription.builder()
                .tenancyId(tenancyId)
                .tenantId(tenantId)
                .razorpayPlanId(result.planId())
                .razorpaySubscriptionId(result.subscriptionId())
                .amountPaise(monthlyAmountPaise)
                .status("CREATED")
                .build();

        TenancySubscription saved = subscriptionRepository.save(subscription);

        kafkaTemplate.send("tenancy.subscription.created", tenancyId.toString(), toJson(Map.of(
                "tenancyId", tenancyId.toString(),
                "tenantId", tenantId.toString(),
                "subscriptionId", result.subscriptionId(),
                "planId", result.planId(),
                "amountPaise", monthlyAmountPaise
        )));

        log.info("Rent subscription created for tenancy {}: sub={}, plan={}",
                tenancyRef, result.subscriptionId(), result.planId());
        return new CreateSubResult(saved, result.shortUrl());
    }

    @Transactional
    public void handleSubscriptionWebhook(String payload, String signature) {
        if (webhookSecret != null && !paymentGateway.verifyWebhook(payload, signature, webhookSecret)) {
            throw new SecurityException("Invalid webhook signature");
        }

        JSONObject event = new JSONObject(payload);
        String eventType = event.getString("event");
        String eventId = event.has("id") ? event.getString("id") : null;

        // Idempotency check
        if (eventId != null && webhookService.isProcessed(eventId)) {
            log.info("Webhook event already processed: {}", eventId);
            return;
        }
        if (eventId != null) {
            webhookService.markProcessing(eventId, "razorpay", eventType);
        }

        // Only handle subscription events
        if (!eventType.startsWith("subscription.")) {
            return;
        }

        JSONObject entityPayload = event.getJSONObject("payload");
        JSONObject subscriptionObj = entityPayload.getJSONObject("subscription").getJSONObject("entity");
        String razorpaySubId = subscriptionObj.getString("id");

        TenancySubscription sub = subscriptionRepository.findByRazorpaySubscriptionId(razorpaySubId)
                .orElse(null);
        if (sub == null) {
            log.debug("No tenancy subscription found for Razorpay sub: {}", razorpaySubId);
            return;
        }

        switch (eventType) {
            case "subscription.authenticated" -> {
                sub.setStatus("AUTHENTICATED");
                kafkaTemplate.send("tenancy.subscription.authenticated",
                        sub.getTenancyId().toString(), toJson(Map.of(
                                "tenancyId", sub.getTenancyId().toString(),
                                "subscriptionId", razorpaySubId
                        )));
                log.info("Subscription authenticated: {}", razorpaySubId);
            }
            case "subscription.activated" -> {
                sub.setStatus("ACTIVE");
                sub.setStartAt(OffsetDateTime.now());
                log.info("Subscription activated: {}", razorpaySubId);
            }
            case "subscription.charged" -> {
                sub.setStatus("ACTIVE");
                sub.setChargeAttempts(0);
                sub.setLastChargedAt(OffsetDateTime.now());
                sub.setFailureReason(null);

                // Extract payment ID from payload
                String paymentId = null;
                if (entityPayload.has("payment")) {
                    paymentId = entityPayload.getJSONObject("payment")
                            .getJSONObject("entity").getString("id");
                }

                kafkaTemplate.send("tenancy.subscription.charged",
                        sub.getTenancyId().toString(), toJson(Map.of(
                                "tenancyId", sub.getTenancyId().toString(),
                                "subscriptionId", razorpaySubId,
                                "paymentId", paymentId != null ? paymentId : "",
                                "amountPaise", sub.getAmountPaise()
                        )));
                log.info("Subscription charged: {} payment: {}", razorpaySubId, paymentId);
            }
            case "subscription.pending" -> {
                sub.setChargeAttempts(sub.getChargeAttempts() + 1);
                sub.setLastFailedAt(OffsetDateTime.now());
                log.warn("Subscription charge pending (attempt {}): {}", sub.getChargeAttempts(), razorpaySubId);
            }
            case "subscription.halted" -> {
                sub.setStatus("HALTED");
                sub.setFailureReason("All charge attempts exhausted");
                kafkaTemplate.send("tenancy.subscription.halted",
                        sub.getTenancyId().toString(), toJson(Map.of(
                                "tenancyId", sub.getTenancyId().toString(),
                                "subscriptionId", razorpaySubId,
                                "reason", "All charge attempts exhausted"
                        )));
                log.error("Subscription halted: {}", razorpaySubId);
            }
            case "subscription.cancelled" -> {
                sub.setStatus("CANCELLED");
                sub.setCancelledAt(OffsetDateTime.now());
                log.info("Subscription cancelled: {}", razorpaySubId);
            }
            case "subscription.completed" -> {
                sub.setStatus("COMPLETED");
                log.info("Subscription completed: {}", razorpaySubId);
            }
            default -> log.debug("Unhandled subscription event: {}", eventType);
        }

        subscriptionRepository.save(sub);
        if (eventId != null) {
            webhookService.markCompleted(eventId);
        }
    }

    @Transactional
    public void cancelSubscription(UUID tenancyId) {
        TenancySubscription sub = subscriptionRepository.findByTenancyId(tenancyId)
                .orElseThrow(() -> new RuntimeException("No subscription found for tenancy: " + tenancyId));

        if ("CANCELLED".equals(sub.getStatus()) || "COMPLETED".equals(sub.getStatus())) {
            log.info("Subscription already {} for tenancy {}", sub.getStatus(), tenancyId);
            return;
        }

        // Cancel on Razorpay — cancel at end of current billing cycle to avoid mid-cycle disruption
        try {
            log.info("Cancelling Razorpay subscription: {}", sub.getRazorpaySubscriptionId());
            razorpayGateway.cancelSubscription(sub.getRazorpaySubscriptionId(), true);
            sub.setStatus("CANCELLED");
            sub.setCancelledAt(OffsetDateTime.now());
            subscriptionRepository.save(sub);
            log.info("Subscription cancelled for tenancy {}: {}", tenancyId, sub.getRazorpaySubscriptionId());
        } catch (Exception e) {
            throw new RuntimeException("Failed to cancel Razorpay subscription: " + e.getMessage(), e);
        }
    }

    public TenancySubscription getByTenancyId(UUID tenancyId) {
        return subscriptionRepository.findByTenancyId(tenancyId)
                .orElseThrow(() -> new RuntimeException("No subscription found for tenancy: " + tenancyId));
    }

    /** Service runs on StringSerializer for Kafka values, so Maps must be stringified first. */
    private static String toJson(Map<String, Object> m) {
        return new JSONObject(m).toString();
    }
}
