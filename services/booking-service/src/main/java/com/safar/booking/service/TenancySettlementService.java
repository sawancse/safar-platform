package com.safar.booking.service;

import com.safar.booking.dto.*;
import com.safar.booking.entity.*;
import com.safar.booking.entity.enums.*;
import com.safar.booking.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class TenancySettlementService {

    private final TenancySettlementRepository settlementRepository;
    private final SettlementDeductionRepository deductionRepository;
    private final PgTenancyRepository tenancyRepository;
    private final TenancyInvoiceRepository invoiceRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RestTemplate restTemplate;
    private final String paymentServiceUrl;

    public TenancySettlementService(
            TenancySettlementRepository settlementRepository,
            SettlementDeductionRepository deductionRepository,
            PgTenancyRepository tenancyRepository,
            TenancyInvoiceRepository invoiceRepository,
            KafkaTemplate<String, Object> kafkaTemplate,
            RestTemplate restTemplate,
            @Value("${services.payment-service.url:http://localhost:8086}") String paymentServiceUrl) {
        this.settlementRepository = settlementRepository;
        this.deductionRepository = deductionRepository;
        this.tenancyRepository = tenancyRepository;
        this.invoiceRepository = invoiceRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.restTemplate = restTemplate;
        this.paymentServiceUrl = paymentServiceUrl;
    }

    private static long settlementCounter = 1000;

    @Transactional
    public TenancySettlement initiateSettlement(UUID tenancyId, InitiateSettlementRequest req) {
        PgTenancy tenancy = tenancyRepository.findById(tenancyId)
                .orElseThrow(() -> new RuntimeException("Tenancy not found: " + tenancyId));

        if (settlementRepository.findByTenancyId(tenancyId).isPresent()) {
            throw new RuntimeException("Settlement already initiated for tenancy: " + tenancyId);
        }

        // Calculate unpaid rent: sum of all GENERATED + OVERDUE invoices
        long unpaidRent = invoiceRepository
                .findByTenancyIdOrderByBillingYearDescBillingMonthDesc(tenancyId, null)
                .stream()
                .filter(inv -> inv.getStatus() == InvoiceStatus.GENERATED || inv.getStatus() == InvoiceStatus.OVERDUE)
                .mapToLong(TenancyInvoice::getGrandTotalPaise)
                .sum();

        // Calculate unpaid utilities from invoices
        long unpaidUtilities = invoiceRepository
                .findByTenancyIdOrderByBillingYearDescBillingMonthDesc(tenancyId, null)
                .stream()
                .filter(inv -> inv.getStatus() == InvoiceStatus.GENERATED || inv.getStatus() == InvoiceStatus.OVERDUE)
                .mapToLong(inv -> inv.getElectricityPaise() + inv.getWaterPaise())
                .sum();

        // Calculate late penalties
        long latePenalties = invoiceRepository
                .findByTenancyIdOrderByBillingYearDescBillingMonthDesc(tenancyId, null)
                .stream()
                .mapToLong(TenancyInvoice::getLatePenaltyPaise)
                .sum();

        long totalDeductions = unpaidRent + unpaidUtilities + latePenalties;
        long deposit = tenancy.getSecurityDepositPaise();
        long refund = Math.max(0, deposit - totalDeductions);
        long additionalDue = Math.max(0, totalDeductions - deposit);

        TenancySettlement settlement = TenancySettlement.builder()
                .tenancyId(tenancyId)
                .settlementRef("STL-" + LocalDate.now().getYear() + "-" + String.format("%04d", ++settlementCounter))
                .moveOutDate(req.moveOutDate() != null ? req.moveOutDate() : LocalDate.now())
                .inspectionDate(req.inspectionDate())
                .securityDepositPaise(deposit)
                .unpaidRentPaise(unpaidRent)
                .unpaidUtilitiesPaise(unpaidUtilities)
                .latePenaltyPaise(latePenalties)
                .totalDeductionsPaise(totalDeductions)
                .refundAmountPaise(refund)
                .additionalDuePaise(additionalDue)
                .status(SettlementStatus.INITIATED)
                .build();

        TenancySettlement saved = settlementRepository.save(settlement);

        // Auto-create deduction line items for unpaid amounts
        if (unpaidRent > 0) {
            deductionRepository.save(SettlementDeduction.builder()
                    .settlement(saved)
                    .category(DeductionCategory.UNPAID_RENT)
                    .description("Outstanding rent from unpaid invoices")
                    .amountPaise(unpaidRent)
                    .build());
        }
        if (unpaidUtilities > 0) {
            deductionRepository.save(SettlementDeduction.builder()
                    .settlement(saved)
                    .category(DeductionCategory.UNPAID_UTILITY)
                    .description("Outstanding electricity and water charges")
                    .amountPaise(unpaidUtilities)
                    .build());
        }
        if (latePenalties > 0) {
            deductionRepository.save(SettlementDeduction.builder()
                    .settlement(saved)
                    .category(DeductionCategory.LATE_PENALTY)
                    .description("Accumulated late payment penalties")
                    .amountPaise(latePenalties)
                    .build());
        }

        kafkaTemplate.send("tenancy.settlement.initiated", saved.getId().toString(), saved);
        log.info("Settlement {} initiated for tenancy {} — deposit: {}, deductions: {}, refund: {}",
                saved.getSettlementRef(), tenancy.getTenancyRef(), deposit, totalDeductions, refund);
        return saved;
    }

    @Transactional
    public SettlementDeduction addDeduction(UUID tenancyId, AddDeductionRequest req) {
        TenancySettlement settlement = getByTenancyId(tenancyId);

        if (settlement.getStatus() != SettlementStatus.INITIATED &&
                settlement.getStatus() != SettlementStatus.INSPECTION_DONE) {
            throw new RuntimeException("Cannot add deductions in status: " + settlement.getStatus());
        }

        SettlementDeduction deduction = SettlementDeduction.builder()
                .settlement(settlement)
                .category(DeductionCategory.valueOf(req.category()))
                .description(req.description())
                .amountPaise(req.amountPaise())
                .evidenceUrl(req.evidenceUrl())
                .build();

        SettlementDeduction saved = deductionRepository.save(deduction);
        recalculateTotals(settlement);
        log.info("Deduction added to settlement {}: {} - {} paise",
                settlement.getSettlementRef(), req.category(), req.amountPaise());
        return saved;
    }

    @Transactional
    public void removeDeduction(UUID tenancyId, UUID deductionId) {
        TenancySettlement settlement = getByTenancyId(tenancyId);
        deductionRepository.deleteById(deductionId);
        recalculateTotals(settlement);
    }

    @Transactional
    public TenancySettlement completeInspection(UUID tenancyId, String inspectionNotes) {
        TenancySettlement settlement = getByTenancyId(tenancyId);

        if (settlement.getStatus() != SettlementStatus.INITIATED) {
            throw new RuntimeException("Settlement must be INITIATED for inspection. Current: " + settlement.getStatus());
        }

        settlement.setInspectionDate(LocalDate.now());
        settlement.setInspectionNotes(inspectionNotes);
        settlement.setStatus(SettlementStatus.INSPECTION_DONE);
        recalculateTotals(settlement);
        return settlementRepository.save(settlement);
    }

    @Transactional
    public TenancySettlement approveSettlement(UUID tenancyId, String role) {
        TenancySettlement settlement = getByTenancyId(tenancyId);

        if (settlement.getStatus() != SettlementStatus.INSPECTION_DONE &&
                settlement.getStatus() != SettlementStatus.INITIATED) {
            throw new RuntimeException("Settlement not ready for approval. Current: " + settlement.getStatus());
        }

        if ("HOST".equalsIgnoreCase(role)) {
            settlement.setApprovedByHostAt(OffsetDateTime.now());
        } else if ("TENANT".equalsIgnoreCase(role)) {
            settlement.setApprovedByTenantAt(OffsetDateTime.now());
        } else {
            throw new RuntimeException("Invalid role for approval: " + role);
        }

        // If both parties approved, move to APPROVED
        if (settlement.getApprovedByHostAt() != null && settlement.getApprovedByTenantAt() != null) {
            settlement.setStatus(SettlementStatus.APPROVED);
            kafkaTemplate.send("tenancy.settlement.approved", settlement.getId().toString(), settlement);
            log.info("Settlement {} approved by both parties", settlement.getSettlementRef());
        }

        return settlementRepository.save(settlement);
    }

    @Transactional
    public TenancySettlement processRefund(UUID tenancyId, UUID hostId, String upiId) {
        TenancySettlement settlement = getByTenancyId(tenancyId);

        if (settlement.getStatus() != SettlementStatus.APPROVED) {
            throw new RuntimeException("Settlement must be APPROVED before refund. Current: " + settlement.getStatus());
        }

        settlement.setStatus(SettlementStatus.REFUND_PROCESSING);
        settlementRepository.save(settlement);

        // If there's a refund amount, initiate payout to tenant via payment-service
        if (settlement.getRefundAmountPaise() > 0 && upiId != null) {
            try {
                String url = paymentServiceUrl + "/api/v1/payments/settlement-payout" +
                        "?tenancyId=" + tenancyId +
                        "&hostId=" + hostId +
                        "&amountPaise=" + settlement.getRefundAmountPaise() +
                        "&upiId=" + upiId;
                @SuppressWarnings("unchecked")
                Map<String, Object> response = restTemplate.postForObject(url, null, Map.class);
                String razorpayPayoutId = response != null ? (String) response.get("razorpayPayoutId") : null;

                if (razorpayPayoutId != null) {
                    settlement.setRazorpayRefundId(razorpayPayoutId);
                    settlementRepository.save(settlement);
                    log.info("Settlement payout initiated for tenancy {}: {}", tenancyId, razorpayPayoutId);
                }
            } catch (Exception e) {
                log.error("Failed to initiate settlement payout for tenancy {}: {}. " +
                        "Settlement stays in REFUND_PROCESSING — retry via admin.", tenancyId, e.getMessage());
            }
        } else if (settlement.getRefundAmountPaise() == 0) {
            // No refund needed — mark as settled directly
            settlement.setStatus(SettlementStatus.SETTLED);
            settlement.setRefundedAt(OffsetDateTime.now());
            settlementRepository.save(settlement);
            log.info("No refund needed for settlement {} — marked SETTLED", settlement.getSettlementRef());
        }

        return settlement;
    }

    @Transactional
    public TenancySettlement markSettled(UUID tenancyId, String razorpayRefundId) {
        TenancySettlement settlement = getByTenancyId(tenancyId);
        settlement.setStatus(SettlementStatus.SETTLED);
        settlement.setRazorpayRefundId(razorpayRefundId);
        settlement.setRefundedAt(OffsetDateTime.now());
        TenancySettlement saved = settlementRepository.save(settlement);

        // Mark tenancy as VACATED
        tenancyRepository.findById(tenancyId).ifPresent(tenancy -> {
            tenancy.setStatus(TenancyStatus.VACATED);
            if (tenancy.getMoveOutDate() == null) {
                tenancy.setMoveOutDate(settlement.getMoveOutDate());
            }
            tenancyRepository.save(tenancy);
        });

        kafkaTemplate.send("tenancy.settled", saved.getId().toString(), saved);
        log.info("Settlement {} marked SETTLED, tenancy vacated", saved.getSettlementRef());
        return saved;
    }

    public TenancySettlement getByTenancyId(UUID tenancyId) {
        return settlementRepository.findByTenancyId(tenancyId)
                .orElseThrow(() -> new RuntimeException("No settlement found for tenancy: " + tenancyId));
    }

    public SettlementResponse toResponse(TenancySettlement s) {
        List<DeductionLineResponse> deductionLines = deductionRepository.findBySettlementId(s.getId())
                .stream()
                .map(d -> new DeductionLineResponse(
                        d.getId(), d.getCategory().name(), d.getDescription(),
                        d.getAmountPaise(), d.getEvidenceUrl()))
                .toList();

        return new SettlementResponse(
                s.getId(), s.getTenancyId(), s.getSettlementRef(),
                s.getMoveOutDate(), s.getInspectionDate(), s.getInspectionNotes(),
                s.getSecurityDepositPaise(),
                s.getUnpaidRentPaise(), s.getUnpaidUtilitiesPaise(),
                s.getDamageDeductionPaise(), s.getLatePenaltyPaise(),
                s.getOtherDeductionsPaise(), s.getOtherDeductionsNote(),
                s.getTotalDeductionsPaise(), s.getRefundAmountPaise(),
                s.getAdditionalDuePaise(),
                s.getStatus().name(),
                s.getApprovedByHostAt(), s.getApprovedByTenantAt(),
                s.getRazorpayRefundId(),
                deductionLines, s.getSettlementPdfUrl(),
                s.getCreatedAt()
        );
    }

    private void recalculateTotals(TenancySettlement settlement) {
        List<SettlementDeduction> deductions = deductionRepository.findBySettlementId(settlement.getId());

        long unpaidRent = sumByCategory(deductions, DeductionCategory.UNPAID_RENT);
        long unpaidUtility = sumByCategory(deductions, DeductionCategory.UNPAID_UTILITY);
        long damage = sumByCategory(deductions, DeductionCategory.DAMAGE);
        long latePenalty = sumByCategory(deductions, DeductionCategory.LATE_PENALTY);
        long cleaning = sumByCategory(deductions, DeductionCategory.CLEANING);
        long other = sumByCategory(deductions, DeductionCategory.OTHER);

        settlement.setUnpaidRentPaise(unpaidRent);
        settlement.setUnpaidUtilitiesPaise(unpaidUtility);
        settlement.setDamageDeductionPaise(damage);
        settlement.setLatePenaltyPaise(latePenalty);
        settlement.setOtherDeductionsPaise(cleaning + other);

        long totalDeductions = unpaidRent + unpaidUtility + damage + latePenalty + cleaning + other;
        settlement.setTotalDeductionsPaise(totalDeductions);

        long deposit = settlement.getSecurityDepositPaise();
        settlement.setRefundAmountPaise(Math.max(0, deposit - totalDeductions));
        settlement.setAdditionalDuePaise(Math.max(0, totalDeductions - deposit));

        settlementRepository.save(settlement);
    }

    private long sumByCategory(List<SettlementDeduction> deductions, DeductionCategory category) {
        return deductions.stream()
                .filter(d -> d.getCategory() == category)
                .mapToLong(SettlementDeduction::getAmountPaise)
                .sum();
    }
}
