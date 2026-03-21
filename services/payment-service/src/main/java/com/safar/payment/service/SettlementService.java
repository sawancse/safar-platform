package com.safar.payment.service;

import com.safar.payment.entity.SettlementLine;
import com.safar.payment.entity.SettlementPlan;
import com.safar.payment.entity.enums.RecipientType;
import com.safar.payment.entity.enums.SettlementStatus;
import com.safar.payment.repository.SettlementLineRepository;
import com.safar.payment.repository.SettlementPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementService {

    private final SettlementPlanRepository settlementPlanRepo;
    private final SettlementLineRepository settlementLineRepo;
    private final LedgerService ledgerService;

    private static final BigDecimal STARTER_COMMISSION = new BigDecimal("0.1800");
    private static final BigDecimal PRO_COMMISSION = new BigDecimal("0.1200");
    private static final BigDecimal COMMERCIAL_COMMISSION = new BigDecimal("0.1000");
    private static final BigDecimal GST_RATE = new BigDecimal("0.1800"); // 18% GST on commission

    @Transactional
    public SettlementPlan createSettlementPlan(UUID bookingId, UUID paymentId, long totalAmountPaise,
                                                String bookingType, UUID hostId, UUID hospitalId,
                                                String hostTier) {
        BigDecimal commissionRate = getCommissionRate(hostTier);
        boolean isMedical = "MEDICAL".equalsIgnoreCase(bookingType);

        SettlementPlan plan = SettlementPlan.builder()
                .bookingId(bookingId)
                .paymentId(paymentId)
                .totalAmountPaise(totalAmountPaise)
                .status(SettlementStatus.PENDING)
                .build();
        plan = settlementPlanRepo.save(plan);

        if (isMedical && hospitalId != null) {
            // For medical bookings, split accommodation vs treatment
            // Assume 50/50 split if not specified — in practice this comes from booking details
            long accommodationPaise = totalAmountPaise / 2;
            long treatmentPaise = totalAmountPaise - accommodationPaise;

            long commissionPaise = calculateCommission(accommodationPaise, commissionRate);
            long gstPaise = calculateGst(commissionPaise);
            long hostPaise = accommodationPaise - commissionPaise - gstPaise;

            createLine(plan, RecipientType.HOST, hostId, hostPaise, commissionRate);
            createLine(plan, RecipientType.HOSPITAL, hospitalId, treatmentPaise, null);
            createLine(plan, RecipientType.PLATFORM, null, commissionPaise, commissionRate);
            createLine(plan, RecipientType.TAX_AUTHORITY, null, gstPaise, null);
        } else {
            long commissionPaise = calculateCommission(totalAmountPaise, commissionRate);
            long gstPaise = calculateGst(commissionPaise);
            long hostPaise = totalAmountPaise - commissionPaise - gstPaise;

            createLine(plan, RecipientType.HOST, hostId, hostPaise, commissionRate);
            createLine(plan, RecipientType.PLATFORM, null, commissionPaise, commissionRate);
            createLine(plan, RecipientType.TAX_AUTHORITY, null, gstPaise, null);
        }

        log.info("Settlement plan created for booking {} with {} lines, total {} paise",
                bookingId, plan.getLines().size(), totalAmountPaise);
        return plan;
    }

    @Transactional
    public SettlementPlan processSettlement(UUID planId) {
        SettlementPlan plan = settlementPlanRepo.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Settlement plan not found: " + planId));

        plan.setStatus(SettlementStatus.PROCESSING);
        settlementPlanRepo.save(plan);

        List<SettlementLine> lines = settlementLineRepo.findByPlanId(planId);
        boolean allCompleted = true;
        boolean anyFailed = false;

        for (SettlementLine line : lines) {
            try {
                line.setStatus(SettlementStatus.PROCESSING);
                settlementLineRepo.save(line);

                // Record ledger entry for each settlement line
                ledgerService.recordEntry(
                        plan.getBookingId(),
                        "SETTLEMENT",
                        "escrow_account",
                        line.getRecipientType().name().toLowerCase() + "_payable",
                        line.getAmountPaise(),
                        "Settlement to " + line.getRecipientType(),
                        line.getId()
                );

                line.setStatus(SettlementStatus.COMPLETED);
                line.setCompletedAt(OffsetDateTime.now());
                settlementLineRepo.save(line);

                log.info("Settlement line completed: recipient={}, amount={} paise",
                        line.getRecipientType(), line.getAmountPaise());
            } catch (Exception e) {
                line.setStatus(SettlementStatus.FAILED);
                line.setFailureReason(e.getMessage());
                settlementLineRepo.save(line);
                allCompleted = false;
                anyFailed = true;
                log.error("Settlement line failed for recipient {}: {}", line.getRecipientType(), e.getMessage());
            }
        }

        if (allCompleted) {
            plan.setStatus(SettlementStatus.COMPLETED);
        } else if (anyFailed && !allCompleted) {
            plan.setStatus(SettlementStatus.PARTIALLY_COMPLETED);
        }
        plan.setCompletedAt(OffsetDateTime.now());
        settlementPlanRepo.save(plan);

        log.info("Settlement plan {} processed with status {}", planId, plan.getStatus());
        return plan;
    }

    public SettlementPlan getSettlementPlan(UUID bookingId) {
        return settlementPlanRepo.findByBookingId(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("No settlement plan found for booking: " + bookingId));
    }

    private SettlementLine createLine(SettlementPlan plan, RecipientType recipientType,
                                       UUID recipientId, long amountPaise, BigDecimal commissionRate) {
        SettlementLine line = SettlementLine.builder()
                .plan(plan)
                .recipientType(recipientType)
                .recipientId(recipientId)
                .amountPaise(amountPaise)
                .commissionRate(commissionRate)
                .status(SettlementStatus.PENDING)
                .build();
        line = settlementLineRepo.save(line);
        plan.getLines().add(line);
        return line;
    }

    private BigDecimal getCommissionRate(String hostTier) {
        if (hostTier == null) return STARTER_COMMISSION;
        return switch (hostTier.toUpperCase()) {
            case "PRO" -> PRO_COMMISSION;
            case "COMMERCIAL" -> COMMERCIAL_COMMISSION;
            default -> STARTER_COMMISSION;
        };
    }

    private long calculateCommission(long amountPaise, BigDecimal rate) {
        return BigDecimal.valueOf(amountPaise).multiply(rate).longValue();
    }

    private long calculateGst(long commissionPaise) {
        return BigDecimal.valueOf(commissionPaise).multiply(GST_RATE).longValue();
    }
}
