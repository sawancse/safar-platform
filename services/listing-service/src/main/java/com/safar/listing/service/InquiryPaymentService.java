package com.safar.listing.service;

import com.safar.listing.dto.InquiryResponse;
import com.safar.listing.entity.PropertyInquiry;
import com.safar.listing.repository.PropertyInquiryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InquiryPaymentService {

    private final PropertyInquiryRepository inquiryRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final RestTemplate restTemplate;

    @Value("${services.payment-service.url:http://localhost:8086}")
    private String paymentServiceUrl;

    /**
     * Create a Razorpay order for the token amount.
     */
    @Transactional
    public Map<String, Object> initiatePayment(UUID inquiryId, UUID buyerId) {
        PropertyInquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new IllegalArgumentException("Inquiry not found"));

        if (!inquiry.getBuyerId().equals(buyerId)) {
            throw new IllegalArgumentException("Only the buyer can pay the token");
        }
        if (inquiry.getTokenAmountPaise() == null || inquiry.getTokenAmountPaise() <= 0) {
            throw new IllegalArgumentException("No token amount set for this inquiry");
        }
        if ("PAID".equals(inquiry.getPaymentStatus())) {
            throw new IllegalStateException("Token already paid");
        }

        // Create Razorpay order via payment-service
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> orderResponse = restTemplate.postForObject(
                    paymentServiceUrl + "/api/v1/payments/create-order",
                    Map.of(
                            "amountPaise", inquiry.getTokenAmountPaise(),
                            "currency", "INR",
                            "receipt", "INQ-" + inquiry.getId().toString().substring(0, 8),
                            "notes", Map.of("inquiryId", inquiry.getId().toString(), "type", "INQUIRY_TOKEN")
                    ),
                    Map.class
            );

            String orderId = orderResponse != null ? String.valueOf(orderResponse.get("orderId")) : null;
            inquiry.setRazorpayOrderId(orderId);
            inquiry.setPaymentStatus("PENDING");
            inquiryRepository.save(inquiry);

            log.info("Token payment order created for inquiry {}: orderId={}", inquiryId, orderId);
            return Map.of(
                    "razorpayOrderId", orderId != null ? orderId : "",
                    "amountPaise", inquiry.getTokenAmountPaise(),
                    "currency", "INR",
                    "inquiryId", inquiry.getId().toString()
            );
        } catch (Exception e) {
            log.error("Failed to create payment order for inquiry {}: {}", inquiryId, e.getMessage());
            throw new RuntimeException("Payment initiation failed: " + e.getMessage());
        }
    }

    /**
     * Confirm payment after Razorpay checkout.
     */
    @Transactional
    public InquiryResponse confirmPayment(UUID inquiryId, String razorpayPaymentId, String razorpaySignature, UUID buyerId) {
        PropertyInquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new IllegalArgumentException("Inquiry not found"));

        if (!inquiry.getBuyerId().equals(buyerId)) {
            throw new IllegalArgumentException("Unauthorized");
        }
        if (!"PENDING".equals(inquiry.getPaymentStatus())) {
            throw new IllegalStateException("Payment not in PENDING state");
        }

        inquiry.setRazorpayPaymentId(razorpayPaymentId);
        inquiry.setPaymentStatus("PAID");
        inquiry.setPaidAt(OffsetDateTime.now());
        PropertyInquiry saved = inquiryRepository.save(inquiry);

        kafkaTemplate.send("inquiry.token.paid", saved.getId().toString());
        log.info("Token paid for inquiry {}: {}", inquiryId, razorpayPaymentId);

        return PropertyInquiryService.toResponse(saved);
    }

    /**
     * Refund token (within 30 days of payment).
     */
    @Transactional
    public InquiryResponse refundToken(UUID inquiryId, UUID buyerId) {
        PropertyInquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new IllegalArgumentException("Inquiry not found"));

        if (!inquiry.getBuyerId().equals(buyerId)) {
            throw new IllegalArgumentException("Unauthorized");
        }
        if (!"PAID".equals(inquiry.getPaymentStatus())) {
            throw new IllegalStateException("Token not in PAID state");
        }
        if (inquiry.getPaidAt() != null && ChronoUnit.DAYS.between(inquiry.getPaidAt(), OffsetDateTime.now()) > 30) {
            throw new IllegalStateException("Refund window expired (30 days)");
        }

        // Initiate refund via payment-service
        try {
            restTemplate.postForObject(
                    paymentServiceUrl + "/api/v1/payments/refund",
                    Map.of("razorpayPaymentId", inquiry.getRazorpayPaymentId(), "amountPaise", inquiry.getTokenAmountPaise()),
                    Map.class
            );
        } catch (Exception e) {
            log.error("Failed to process refund for inquiry {}: {}", inquiryId, e.getMessage());
            throw new RuntimeException("Refund failed: " + e.getMessage());
        }

        inquiry.setPaymentStatus("REFUNDED");
        inquiry.setRefundedAt(OffsetDateTime.now());
        PropertyInquiry saved = inquiryRepository.save(inquiry);

        kafkaTemplate.send("inquiry.token.refunded", saved.getId().toString());
        log.info("Token refunded for inquiry {}", inquiryId);

        return PropertyInquiryService.toResponse(saved);
    }
}
