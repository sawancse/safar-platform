package com.safar.payment.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safar.payment.entity.Payment;
import com.safar.payment.entity.enums.RefundType;
import com.safar.payment.repository.PaymentRepository;
import com.safar.payment.service.RefundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Consumes chef booking refund requests and processes refunds via Razorpay.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChefRefundConsumer {

    private final RefundService refundService;
    private final PaymentRepository paymentRepo;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "chef.booking.refund.requested", groupId = "payment-chef-refund-group")
    public void onChefRefundRequested(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            String bookingId = node.has("bookingId") ? node.get("bookingId").asText() : null;
            String razorpayPaymentId = node.has("razorpayPaymentId") ? node.get("razorpayPaymentId").asText() : null;
            long amountPaise = node.has("amountPaise") ? node.get("amountPaise").asLong() : 0;
            String reason = node.has("reason") ? node.get("reason").asText() : "Chef booking cancelled";
            String bookingRef = node.has("bookingRef") ? node.get("bookingRef").asText() : "";

            if (razorpayPaymentId == null || amountPaise <= 0) {
                log.warn("Chef refund skipped — missing razorpayPaymentId or amountPaise: bookingId={}", bookingId);
                return;
            }

            // Find payment by Razorpay payment ID
            Payment payment = paymentRepo.findByRazorpayPaymentId(razorpayPaymentId).orElse(null);
            if (payment == null) {
                log.warn("Chef refund skipped — payment not found for razorpayPaymentId={}, bookingRef={}",
                        razorpayPaymentId, bookingRef);
                return;
            }

            refundService.initiateRefund(
                    payment.getId(),
                    bookingId != null ? UUID.fromString(bookingId) : null,
                    amountPaise,
                    reason,
                    RefundType.CHEF_BOOKING
            );

            log.info("Chef booking refund processed: bookingRef={} amount={} paise", bookingRef, amountPaise);
        } catch (Exception e) {
            log.error("Error processing chef refund request: {}", e.getMessage(), e);
        }
    }
}
