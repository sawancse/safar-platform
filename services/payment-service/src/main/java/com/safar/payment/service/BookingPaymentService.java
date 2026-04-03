package com.safar.payment.service;

import com.safar.payment.dto.PaymentOrderResponse;
import com.safar.payment.entity.Payment;
import com.safar.payment.entity.Payout;
import com.safar.payment.entity.enums.PaymentStatus;
import com.safar.payment.entity.enums.PayoutStatus;
import com.safar.payment.repository.PaymentRepository;
import com.safar.payment.repository.PayoutRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingPaymentService {

    private final RazorpayGateway razorpayGateway;
    private final PaymentRepository paymentRepo;
    private final PayoutRepository payoutRepo;
    private final KafkaTemplate<String, String> kafka;

    @Value("${razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${razorpay.webhook-secret}")
    private String webhookSecret;

    @Transactional
    public PaymentOrderResponse createOrder(UUID bookingId, Long amountPaise) throws Exception {
        // If amountPaise not provided, look up from existing payment or fail
        if (amountPaise == null) {
            throw new IllegalArgumentException("amountPaise is required");
        }

        String orderId = razorpayGateway.createOrder(amountPaise, bookingId.toString());

        Payment payment = Payment.builder()
                .bookingId(bookingId)
                .razorpayOrderId(orderId)
                .amountPaise(amountPaise)
                .status(PaymentStatus.CREATED)
                .build();
        paymentRepo.save(payment);

        return new PaymentOrderResponse(orderId, amountPaise, "INR", razorpayKeyId);
    }

    @Transactional
    public void verifyPayment(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
        if (!razorpayGateway.verifyPaymentSignature(razorpayOrderId, razorpayPaymentId, razorpaySignature)) {
            throw new SecurityException("Invalid payment signature");
        }

        Payment payment = paymentRepo.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found for order: " + razorpayOrderId));

        if (payment.getStatus() == PaymentStatus.CAPTURED) {
            return; // Already processed via webhook
        }

        payment.setRazorpayPaymentId(razorpayPaymentId);
        payment.setStatus(PaymentStatus.CAPTURED);
        payment.setCapturedAt(OffsetDateTime.now());
        paymentRepo.save(payment);

        kafka.send("payment.captured", payment.getBookingId().toString());
        log.info("Payment verified and captured for booking {}", payment.getBookingId());
    }

    @Transactional
    public void handlePaymentWebhook(String payload, String signature) {
        if (!razorpayGateway.verifyWebhookSignature(payload, signature, webhookSecret)) {
            throw new SecurityException("Invalid webhook signature");
        }

        String eventType = extractField(payload, "event");

        // Handle payout events (RazorpayX)
        if (eventType.startsWith("payout.")) {
            handlePayoutWebhook(payload, eventType);
            return;
        }

        String orderId = extractField(payload, "order_id");

        if ("payment.captured".equals(eventType)) {
            handlePaymentCapture(orderId);
        } else if ("payment.failed".equals(eventType)) {
            handlePaymentFailed(orderId);
        }
    }

    private void handlePaymentCapture(String orderId) {
        paymentRepo.findByRazorpayOrderId(orderId).ifPresent(payment -> {
            payment.setStatus(PaymentStatus.CAPTURED);
            payment.setCapturedAt(OffsetDateTime.now());
            paymentRepo.save(payment);
            kafka.send("payment.captured", payment.getBookingId().toString());
            log.info("Payment captured for booking {}", payment.getBookingId());
        });
    }

    private void handlePayoutWebhook(String payload, String eventType) {
        try {
            JSONObject root = new JSONObject(payload);
            JSONObject payoutEntity = root.getJSONObject("payload")
                    .getJSONObject("payout")
                    .getJSONObject("entity");

            String razorpayPayoutId = payoutEntity.getString("id");
            String status = payoutEntity.optString("status", "");
            String utr = payoutEntity.optString("utr", null);

            payoutRepo.findByRazorpayPayoutId(razorpayPayoutId).ifPresentOrElse(payout -> {
                switch (eventType) {
                    case "payout.processed" -> {
                        payout.setStatus(PayoutStatus.COMPLETED);
                        payout.setCompletedAt(OffsetDateTime.now());
                        payoutRepo.save(payout);
                        kafka.send("payout.completed", payout.getHostId().toString());
                        log.info("Payout completed: {} (UTR: {}) for host {}",
                                razorpayPayoutId, utr, payout.getHostId());
                    }
                    case "payout.reversed" -> {
                        payout.setStatus(PayoutStatus.REVERSED);
                        payout.setFailureReason("Payout reversed by bank. Status: " + status);
                        payoutRepo.save(payout);
                        kafka.send("payout.reversed", payout.getHostId().toString());
                        log.warn("Payout reversed: {} for host {}", razorpayPayoutId, payout.getHostId());
                    }
                    case "payout.failed" -> {
                        String failureReason = payoutEntity.optString("failure_reason", "Unknown");
                        payout.setStatus(PayoutStatus.FAILED);
                        payout.setFailureReason(failureReason);
                        payout.setRetryCount(payout.getRetryCount() + 1);
                        payoutRepo.save(payout);
                        kafka.send("payout.failed", payout.getHostId().toString());
                        log.error("Payout failed: {} for host {} — {}",
                                razorpayPayoutId, payout.getHostId(), failureReason);
                    }
                    default -> log.debug("Unhandled payout event: {}", eventType);
                }
            }, () -> log.warn("No payout found for Razorpay payout ID: {}", razorpayPayoutId));
        } catch (Exception e) {
            log.error("Failed to process payout webhook: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public Payout retryPayout(UUID payoutId) {
        Payout payout = payoutRepo.findById(payoutId)
                .orElseThrow(() -> new RuntimeException("Payout not found: " + payoutId));

        if (payout.getStatus() != PayoutStatus.FAILED && payout.getStatus() != PayoutStatus.REVERSED) {
            throw new RuntimeException("Can only retry FAILED or REVERSED payouts. Current: " + payout.getStatus());
        }

        try {
            String razorpayPayoutId = razorpayGateway.createPayout(
                    payout.getUpiId(), payout.getNetAmountPaise(), "payout",
                    "retry-" + payout.getId().toString().substring(0, 8));
            payout.setRazorpayPayoutId(razorpayPayoutId);
            payout.setStatus(PayoutStatus.PROCESSING);
            payout.setFailureReason(null);
            payout.setInitiatedAt(OffsetDateTime.now());
            log.info("Payout retried: {} → {}", payoutId, razorpayPayoutId);
        } catch (Exception e) {
            payout.setFailureReason("Retry failed: " + e.getMessage());
            payout.setRetryCount(payout.getRetryCount() + 1);
            log.error("Payout retry failed for {}: {}", payoutId, e.getMessage());
        }

        return payoutRepo.save(payout);
    }

    private void handlePaymentFailed(String orderId) {
        paymentRepo.findByRazorpayOrderId(orderId).ifPresent(payment -> {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepo.save(payment);
            kafka.send("payment.failed", payment.getBookingId().toString());
            log.warn("Payment failed for booking {}", payment.getBookingId());
        });
    }

    @Transactional
    public Payout initiateHostPayout(UUID hostId, UUID bookingId, Long amountPaise, String upiId) {
        Payout payout = Payout.builder()
                .hostId(hostId)
                .bookingId(bookingId)
                .amountPaise(amountPaise)
                .netAmountPaise(amountPaise)
                .method("UPI")
                .upiId(upiId)
                .status(PayoutStatus.PENDING)
                .initiatedAt(OffsetDateTime.now())
                .build();
        Payout saved = payoutRepo.save(payout);

        // Attempt Razorpay payout
        try {
            String razorpayPayoutId = razorpayGateway.createPayout(
                    upiId, amountPaise, "payout",
                    "booking-" + bookingId.toString().substring(0, 8));
            saved.setRazorpayPayoutId(razorpayPayoutId);
            saved.setStatus(PayoutStatus.PROCESSING);
            payoutRepo.save(saved);
            log.info("Payout initiated for host {} booking {}: {}", hostId, bookingId, razorpayPayoutId);
        } catch (Exception e) {
            saved.setFailureReason(e.getMessage());
            saved.setStatus(PayoutStatus.FAILED);
            saved.setRetryCount(saved.getRetryCount() + 1);
            payoutRepo.save(saved);
            log.error("Payout failed for host {} booking {}: {}", hostId, bookingId, e.getMessage());
        }

        return saved;
    }

    @Transactional
    public Payout initiateSettlementPayout(UUID hostId, UUID tenancyId, Long amountPaise, String upiId) {
        Payout payout = Payout.builder()
                .hostId(hostId)
                .amountPaise(amountPaise)
                .netAmountPaise(amountPaise)
                .method("UPI")
                .upiId(upiId)
                .status(PayoutStatus.PENDING)
                .initiatedAt(OffsetDateTime.now())
                .build();
        Payout saved = payoutRepo.save(payout);

        try {
            String razorpayPayoutId = razorpayGateway.createPayout(
                    upiId, amountPaise, "refund",
                    "settlement-" + tenancyId.toString().substring(0, 8));
            saved.setRazorpayPayoutId(razorpayPayoutId);
            saved.setStatus(PayoutStatus.PROCESSING);
            payoutRepo.save(saved);
            log.info("Settlement payout initiated for tenancy {}: {}", tenancyId, razorpayPayoutId);
        } catch (Exception e) {
            saved.setFailureReason(e.getMessage());
            saved.setStatus(PayoutStatus.FAILED);
            saved.setRetryCount(saved.getRetryCount() + 1);
            payoutRepo.save(saved);
            log.error("Settlement payout failed for tenancy {}: {}", tenancyId, e.getMessage());
        }

        return saved;
    }

    private String extractField(String json, String field) {
        try {
            JSONObject root = new JSONObject(json);
            if ("event".equals(field)) {
                return root.optString("event", "");
            }
            if ("order_id".equals(field)) {
                return root.getJSONObject("payload")
                        .getJSONObject("payment")
                        .getJSONObject("entity")
                        .optString("order_id", "");
            }
            return root.optString(field, "");
        } catch (Exception e) {
            log.warn("Failed to extract '{}' from webhook payload: {}", field, e.getMessage());
            return "";
        }
    }
}
