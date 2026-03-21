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
                .build();
        return payoutRepo.save(payout);
    }

    private String extractField(String json, String field) {
        int idx = json.indexOf("\"" + field + "\"");
        if (idx < 0) return "";
        int colon = json.indexOf(":", idx);
        int start = json.indexOf("\"", colon + 1);
        int end = json.indexOf("\"", start + 1);
        if (start >= 0 && end > start) {
            return json.substring(start + 1, end);
        }
        return "";
    }
}
