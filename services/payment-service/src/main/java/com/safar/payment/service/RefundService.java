package com.safar.payment.service;

import com.safar.payment.dto.RefundRequest;
import com.safar.payment.entity.Payment;
import com.safar.payment.entity.RefundRecord;
import com.safar.payment.entity.enums.RefundStatus;
import com.safar.payment.entity.enums.RefundType;
import com.safar.payment.repository.PaymentRepository;
import com.safar.payment.repository.RefundRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefundService {

    private final RefundRecordRepository refundRecordRepo;
    private final PaymentRepository paymentRepo;
    private final RazorpayGateway razorpayGateway;
    private final LedgerService ledgerService;
    private final KafkaTemplate<String, String> kafka;

    /**
     * Initiates a refund through the payment gateway.
     */
    @Transactional
    public RefundRecord initiateRefund(UUID paymentId, UUID bookingId, long amountPaise,
                                       String reason, RefundType type) {
        // Try by paymentId first, fall back to bookingId lookup
        Payment payment = paymentRepo.findById(paymentId)
                .or(() -> bookingId != null ? paymentRepo.findFirstByBookingIdOrderByCreatedAtDesc(bookingId) : java.util.Optional.empty())
                .orElseThrow(() -> new IllegalArgumentException("Payment not found for paymentId=" + paymentId + " or bookingId=" + bookingId));

        RefundRecord refund = RefundRecord.builder()
                .paymentId(paymentId)
                .bookingId(bookingId)
                .amountPaise(amountPaise)
                .reason(reason)
                .refundType(type)
                .status(RefundStatus.INITIATED)
                .build();
        refund = refundRecordRepo.save(refund);

        try {
            refund.setStatus(RefundStatus.PROCESSING);
            refundRecordRepo.save(refund);

            String gatewayRefundId = razorpayGateway.refund(payment.getRazorpayPaymentId(), amountPaise);
            refund.setGatewayRefundId(gatewayRefundId);
            refund.setStatus(RefundStatus.COMPLETED);
            refund.setCompletedAt(OffsetDateTime.now());
            refundRecordRepo.save(refund);

            ledgerService.recordEntry(
                    bookingId,
                    "REFUND",
                    "escrow_account",
                    "guest_receivable",
                    amountPaise,
                    "Refund: " + reason + " (" + type + ")",
                    refund.getId()
            );

            kafka.send("payment.refunded", bookingId != null ? bookingId.toString() : paymentId.toString());
            log.info("Refund completed for payment {}: {} paise, reason={}", paymentId, amountPaise, reason);

        } catch (Exception e) {
            refund.setStatus(RefundStatus.FAILED);
            refund.setFailureReason(e.getMessage());
            refundRecordRepo.save(refund);
            log.error("Refund failed for payment {}: {}", paymentId, e.getMessage());
        }

        return refund;
    }

    /**
     * Convenience method accepting RefundRequest DTO.
     */
    @Transactional
    public RefundRecord initiateRefund(RefundRequest req) {
        return initiateRefund(
                req.paymentId(),
                req.bookingId(),
                req.amountPaise(),
                req.reason(),
                RefundType.valueOf(req.refundType())
        );
    }

    public List<RefundRecord> getRefunds(UUID bookingId) {
        return refundRecordRepo.findByBookingId(bookingId);
    }

    /**
     * Calculates refund amount based on cancellation policy and time before check-in.
     *
     * @param totalPaise total payment amount in paise
     * @param cancellationPolicy FREE, MODERATE, or STRICT
     * @param hoursBeforeCheckin hours remaining before check-in
     * @return refund amount in paise
     */
    public long calculateRefundAmount(long totalPaise, String cancellationPolicy, long hoursBeforeCheckin) {
        if (cancellationPolicy == null) {
            cancellationPolicy = "MODERATE";
        }

        return switch (cancellationPolicy.toUpperCase()) {
            case "FREE" -> {
                if (hoursBeforeCheckin > 48) {
                    yield totalPaise; // 100% refund
                } else {
                    yield totalPaise / 2; // 50% if within 48h even on free policy
                }
            }
            case "MODERATE" -> {
                if (hoursBeforeCheckin > 48) {
                    yield totalPaise; // 100%
                } else if (hoursBeforeCheckin >= 24) {
                    yield totalPaise / 2; // 50%
                } else {
                    yield 0L; // No refund
                }
            }
            case "STRICT" -> {
                if (hoursBeforeCheckin > 48) {
                    yield totalPaise / 2; // 50%
                } else {
                    // Only platform fee refunded (roughly 0% of booking)
                    yield 0L;
                }
            }
            default -> 0L;
        };
    }

    /**
     * Calculates medical tourism refund amounts.
     *
     * @param treatmentPaise treatment cost in paise
     * @param accommodationPaise accommodation cost in paise
     * @param daysBeforeProcedure days remaining before medical procedure
     * @return total refund amount in paise
     */
    public long calculateMedicalRefund(long treatmentPaise, long accommodationPaise, long daysBeforeProcedure) {
        long treatmentRefund;
        long accommodationRefund;

        if (daysBeforeProcedure > 7) {
            // >7 days: 80% treatment + 100% accommodation
            treatmentRefund = (treatmentPaise * 80) / 100;
            accommodationRefund = accommodationPaise;
        } else if (daysBeforeProcedure >= 3) {
            // 3-7 days: 50% treatment + 50% accommodation
            treatmentRefund = treatmentPaise / 2;
            accommodationRefund = accommodationPaise / 2;
        } else {
            // <3 days: 0% treatment + accommodation minus 1 night
            treatmentRefund = 0;
            long perNightEstimate = accommodationPaise > 0
                    ? accommodationPaise / Math.max(daysBeforeProcedure + 1, 1)
                    : 0;
            accommodationRefund = Math.max(0, accommodationPaise - perNightEstimate);
        }

        return treatmentRefund + accommodationRefund;
    }
}
