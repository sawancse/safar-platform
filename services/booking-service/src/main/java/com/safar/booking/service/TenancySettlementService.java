package com.safar.booking.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safar.booking.dto.*;
import com.safar.booking.entity.*;
import com.safar.booking.entity.enums.*;
import com.safar.booking.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

@Service
@Slf4j
public class TenancySettlementService {

    private final TenancySettlementRepository settlementRepository;
    private final SettlementDeductionRepository deductionRepository;
    private final InspectionChecklistItemRepository checklistRepository;
    private final PgTenancyRepository tenancyRepository;
    private final TenancyInvoiceRepository invoiceRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final String paymentServiceUrl;

    public TenancySettlementService(
            TenancySettlementRepository settlementRepository,
            SettlementDeductionRepository deductionRepository,
            InspectionChecklistItemRepository checklistRepository,
            PgTenancyRepository tenancyRepository,
            TenancyInvoiceRepository invoiceRepository,
            KafkaTemplate<String, Object> kafkaTemplate,
            ObjectMapper objectMapper,
            RestTemplate restTemplate,
            @Value("${services.payment-service.url:http://localhost:8086}") String paymentServiceUrl) {
        this.settlementRepository = settlementRepository;
        this.deductionRepository = deductionRepository;
        this.checklistRepository = checklistRepository;
        this.tenancyRepository = tenancyRepository;
        this.invoiceRepository = invoiceRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
        this.paymentServiceUrl = paymentServiceUrl;
    }

    /** Kafka producer uses StringSerializer — JSON-stringify before send. */
    private void sendEvent(String topic, String key, Object payload, UUID entityId) {
        try {
            kafkaTemplate.send(topic, key, objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            log.warn("Kafka {} payload serialization failed for {}: {}", topic, entityId, e.getMessage());
            kafkaTemplate.send(topic, key, "{\"id\":\"" + entityId + "\"}");
        }
    }

    private static long settlementCounter = 1000;

    @Transactional
    public TenancySettlement initiateSettlement(UUID tenancyId, InitiateSettlementRequest req) {
        PgTenancy tenancy = tenancyRepository.findById(tenancyId)
                .orElseThrow(() -> new RuntimeException("Tenancy not found: " + tenancyId));

        if (settlementRepository.findByTenancyId(tenancyId).isPresent()) {
            throw new RuntimeException("Settlement already initiated for tenancy: " + tenancyId);
        }

        long unpaidRent = invoiceRepository
                .findByTenancyIdOrderByBillingYearDescBillingMonthDesc(tenancyId, null)
                .stream()
                .filter(inv -> inv.getStatus() == InvoiceStatus.GENERATED || inv.getStatus() == InvoiceStatus.OVERDUE)
                .mapToLong(TenancyInvoice::getGrandTotalPaise)
                .sum();

        long unpaidUtilities = invoiceRepository
                .findByTenancyIdOrderByBillingYearDescBillingMonthDesc(tenancyId, null)
                .stream()
                .filter(inv -> inv.getStatus() == InvoiceStatus.GENERATED || inv.getStatus() == InvoiceStatus.OVERDUE)
                .mapToLong(inv -> inv.getElectricityPaise() + inv.getWaterPaise())
                .sum();

        long latePenalties = invoiceRepository
                .findByTenancyIdOrderByBillingYearDescBillingMonthDesc(tenancyId, null)
                .stream()
                .mapToLong(TenancyInvoice::getLatePenaltyPaise)
                .sum();

        long totalDeductions = unpaidRent + unpaidUtilities + latePenalties;
        long deposit = tenancy.getSecurityDepositPaise();
        long refund = Math.max(0, deposit - totalDeductions);
        long additionalDue = Math.max(0, totalDeductions - deposit);

        LocalDate moveOut = req.moveOutDate() != null ? req.moveOutDate() : LocalDate.now();

        TenancySettlement settlement = TenancySettlement.builder()
                .tenancyId(tenancyId)
                .settlementRef("STL-" + LocalDate.now().getYear() + "-" + String.format("%04d", ++settlementCounter))
                .moveOutDate(moveOut)
                .inspectionDate(req.inspectionDate())
                .securityDepositPaise(deposit)
                .unpaidRentPaise(unpaidRent)
                .unpaidUtilitiesPaise(unpaidUtilities)
                .latePenaltyPaise(latePenalties)
                .totalDeductionsPaise(totalDeductions)
                .refundAmountPaise(refund)
                .additionalDuePaise(additionalDue)
                .status(SettlementStatus.INITIATED)
                // Refund deadline: 21 days from move-out
                .refundDeadlineDays(21)
                .refundDeadlineDate(moveOut.plusDays(21))
                .build();

        TenancySettlement saved = settlementRepository.save(settlement);

        if (unpaidRent > 0) {
            deductionRepository.save(SettlementDeduction.builder()
                    .settlement(saved).category(DeductionCategory.UNPAID_RENT)
                    .description("Outstanding rent from unpaid invoices").amountPaise(unpaidRent).build());
        }
        if (unpaidUtilities > 0) {
            deductionRepository.save(SettlementDeduction.builder()
                    .settlement(saved).category(DeductionCategory.UNPAID_UTILITY)
                    .description("Outstanding electricity and water charges").amountPaise(unpaidUtilities).build());
        }
        if (latePenalties > 0) {
            deductionRepository.save(SettlementDeduction.builder()
                    .settlement(saved).category(DeductionCategory.LATE_PENALTY)
                    .description("Accumulated late payment penalties").amountPaise(latePenalties).build());
        }

        sendEvent("tenancy.settlement.initiated", saved.getId().toString(), saved, saved.getId());
        log.info("Settlement {} initiated for tenancy {} — deposit: {}, deductions: {}, refund: {}, deadline: {}",
                saved.getSettlementRef(), tenancy.getTenancyRef(), deposit, totalDeductions, refund, saved.getRefundDeadlineDate());
        return saved;
    }

    // ── Deductions ──────────────────────────────────────────────

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

    // ── Inspection Checklist ──────────────────────────────────────

    @Transactional
    public InspectionChecklistItemResponse addChecklistItem(UUID tenancyId, InspectionChecklistItemDto dto) {
        TenancySettlement settlement = getByTenancyId(tenancyId);

        if (settlement.getStatus() != SettlementStatus.INITIATED &&
                settlement.getStatus() != SettlementStatus.INSPECTION_DONE) {
            throw new RuntimeException("Cannot add checklist items in status: " + settlement.getStatus());
        }

        InspectionChecklistItem item = InspectionChecklistItem.builder()
                .settlement(settlement)
                .area(dto.area())
                .itemName(dto.itemName())
                .condition(dto.condition())
                .damageDescription(dto.damageDescription())
                .photoUrls(dto.photoUrls())
                .deductionPaise(dto.deductionPaise())
                .build();

        InspectionChecklistItem saved = checklistRepository.save(item);

        // Auto-create deduction if DAMAGED or MISSING with deduction amount
        if (("DAMAGED".equals(dto.condition()) || "MISSING".equals(dto.condition())) && dto.deductionPaise() > 0) {
            deductionRepository.save(SettlementDeduction.builder()
                    .settlement(settlement)
                    .category(DeductionCategory.DAMAGE)
                    .description(dto.area() + " - " + dto.itemName() + ": " + dto.condition())
                    .amountPaise(dto.deductionPaise())
                    .evidenceUrl(dto.photoUrls())
                    .build());
            recalculateTotals(settlement);
        }

        log.info("Checklist item added: {} - {} ({})", dto.area(), dto.itemName(), dto.condition());
        return toChecklistResponse(saved);
    }

    public List<InspectionChecklistItemResponse> getChecklistItems(UUID tenancyId) {
        TenancySettlement settlement = getByTenancyId(tenancyId);
        return checklistRepository.findBySettlementIdOrderByCreatedAtAsc(settlement.getId())
                .stream().map(this::toChecklistResponse).toList();
    }

    @Transactional
    public void removeChecklistItem(UUID tenancyId, UUID itemId) {
        checklistRepository.deleteById(itemId);
    }

    // ── Inspection ──────────────────────────────────────────────

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

    // ── Dispute ──────────────────────────────────────────────

    @Transactional
    public void disputeDeduction(UUID tenancyId, UUID deductionId, String reason) {
        TenancySettlement settlement = getByTenancyId(tenancyId);
        SettlementDeduction deduction = deductionRepository.findById(deductionId)
                .orElseThrow(() -> new RuntimeException("Deduction not found: " + deductionId));

        deduction.setDisputed(true);
        deduction.setDisputeReason(reason);
        deductionRepository.save(deduction);

        settlement.setStatus(SettlementStatus.DISPUTED);
        settlement.setDisputeReason("Deduction disputed: " + deduction.getCategory().name());
        settlement.setDisputeRaisedAt(OffsetDateTime.now());
        settlementRepository.save(settlement);

        sendEvent("settlement.disputed", settlement.getId().toString(), settlement, settlement.getId());
        log.info("Deduction {} disputed in settlement {}: {}", deductionId, settlement.getSettlementRef(), reason);
    }

    @Transactional
    public void disputeSettlement(UUID tenancyId, String reason) {
        TenancySettlement settlement = getByTenancyId(tenancyId);
        settlement.setStatus(SettlementStatus.DISPUTED);
        settlement.setDisputeReason(reason);
        settlement.setDisputeRaisedAt(OffsetDateTime.now());
        settlementRepository.save(settlement);

        sendEvent("settlement.disputed", settlement.getId().toString(), settlement, settlement.getId());
        log.info("Settlement {} disputed by tenant: {}", settlement.getSettlementRef(), reason);
    }

    // ── Admin Dispute Resolution ──────────────────────────────

    @Transactional
    public void adminResolveDispute(UUID settlementId, AdminResolveDisputeRequest req) {
        TenancySettlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new RuntimeException("Settlement not found: " + settlementId));

        SettlementDeduction deduction = deductionRepository.findById(req.deductionId())
                .orElseThrow(() -> new RuntimeException("Deduction not found: " + req.deductionId()));

        deduction.setDisputeResolved(true);
        deduction.setAdminDecision(req.decision());
        deduction.setAdminNotes(req.notes());

        if ("REMOVED".equals(req.decision())) {
            deduction.setAdminAdjustedPaise(0L);
            deduction.setAmountPaise(0);
        } else if ("REDUCED".equals(req.decision()) && req.adjustedPaise() != null) {
            deduction.setAdminAdjustedPaise(req.adjustedPaise());
            deduction.setAmountPaise(req.adjustedPaise());
        } else {
            // UPHELD — keep original amount
            deduction.setAdminAdjustedPaise(deduction.getAmountPaise());
        }
        deductionRepository.save(deduction);

        // Check if all disputes resolved
        List<SettlementDeduction> allDeductions = deductionRepository.findBySettlementId(settlement.getId());
        boolean allResolved = allDeductions.stream()
                .filter(SettlementDeduction::isDisputed)
                .allMatch(SettlementDeduction::isDisputeResolved);

        if (allResolved && settlement.getStatus() == SettlementStatus.DISPUTED) {
            settlement.setStatus(SettlementStatus.ADMIN_RESOLVED);
            sendEvent("settlement.admin.resolved", settlement.getId().toString(), settlement, settlement.getId());
        }

        recalculateTotals(settlement);
        log.info("Admin resolved dispute for deduction {} in settlement {}: {} → {}",
                req.deductionId(), settlement.getSettlementRef(), req.decision(),
                req.adjustedPaise() != null ? req.adjustedPaise() + " paise" : "original");
    }

    @Transactional
    public TenancySettlement adminOverrideSettlement(UUID settlementId, UUID adminId, AdminOverrideRequest req) {
        TenancySettlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new RuntimeException("Settlement not found: " + settlementId));

        settlement.setAdminOverrideAt(OffsetDateTime.now());
        settlement.setAdminOverrideBy(adminId);
        settlement.setAdminOverrideNotes(req.notes());

        if (req.overrideRefundPaise() != null) {
            settlement.setRefundAmountPaise(req.overrideRefundPaise());
            long deposit = settlement.getSecurityDepositPaise();
            settlement.setAdditionalDuePaise(Math.max(0, settlement.getTotalDeductionsPaise() - deposit));
        }

        settlement.setStatus(SettlementStatus.APPROVED);
        settlement.setApprovedByHostAt(OffsetDateTime.now());
        settlement.setApprovedByTenantAt(OffsetDateTime.now());

        TenancySettlement saved = settlementRepository.save(settlement);
        sendEvent("settlement.admin.resolved", saved.getId().toString(), saved, saved.getId());
        log.info("Admin override on settlement {}: refund={} paise", saved.getSettlementRef(), saved.getRefundAmountPaise());
        return saved;
    }

    // ── Bank Details ──────────────────────────────────────────

    @Transactional
    public void saveBankDetails(UUID tenancyId, TenantBankDetailsRequest req) {
        TenancySettlement settlement = getByTenancyId(tenancyId);
        if (req.bankAccount() != null) settlement.setTenantBankAccount(req.bankAccount());
        if (req.ifsc() != null) settlement.setTenantIfsc(req.ifsc());
        if (req.upiId() != null) settlement.setTenantUpiId(req.upiId());
        settlementRepository.save(settlement);
    }

    // ── Approval & Refund (existing, enhanced) ──────────────────

    @Transactional
    public TenancySettlement approveSettlement(UUID tenancyId, String role) {
        TenancySettlement settlement = getByTenancyId(tenancyId);

        if (settlement.getStatus() != SettlementStatus.INSPECTION_DONE &&
                settlement.getStatus() != SettlementStatus.INITIATED &&
                settlement.getStatus() != SettlementStatus.ADMIN_RESOLVED) {
            throw new RuntimeException("Settlement not ready for approval. Current: " + settlement.getStatus());
        }

        if ("HOST".equalsIgnoreCase(role)) {
            settlement.setApprovedByHostAt(OffsetDateTime.now());
        } else if ("TENANT".equalsIgnoreCase(role)) {
            settlement.setApprovedByTenantAt(OffsetDateTime.now());
        } else {
            throw new RuntimeException("Invalid role for approval: " + role);
        }

        if (settlement.getApprovedByHostAt() != null && settlement.getApprovedByTenantAt() != null) {
            settlement.setStatus(SettlementStatus.APPROVED);
            sendEvent("tenancy.settlement.approved", settlement.getId().toString(), settlement, settlement.getId());
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

        // Use tenant's saved UPI if not provided
        String refundUpi = upiId != null ? upiId : settlement.getTenantUpiId();

        settlementRepository.save(settlement);

        if (settlement.getRefundAmountPaise() > 0 && refundUpi != null) {
            try {
                String url = paymentServiceUrl + "/api/v1/payments/settlement-payout" +
                        "?tenancyId=" + tenancyId +
                        "&hostId=" + hostId +
                        "&amountPaise=" + settlement.getRefundAmountPaise() +
                        "&upiId=" + refundUpi;
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

        tenancyRepository.findById(tenancyId).ifPresent(tenancy -> {
            tenancy.setStatus(TenancyStatus.VACATED);
            if (tenancy.getMoveOutDate() == null) {
                tenancy.setMoveOutDate(settlement.getMoveOutDate());
            }
            tenancyRepository.save(tenancy);
        });

        sendEvent("tenancy.settled", saved.getId().toString(), saved, saved.getId());
        log.info("Settlement {} marked SETTLED, tenancy vacated", saved.getSettlementRef());
        return saved;
    }

    // ── Timeline ──────────────────────────────────────────────

    public List<SettlementTimelineResponse> getTimeline(UUID tenancyId) {
        TenancySettlement s = getByTenancyId(tenancyId);
        List<SettlementTimelineResponse> timeline = new ArrayList<>();
        String currentStatus = s.getStatus().name();

        timeline.add(new SettlementTimelineResponse("INITIATED", "Settlement Initiated", s.getCreatedAt(), "INITIATED".equals(currentStatus)));

        timeline.add(new SettlementTimelineResponse("INSPECTION_DONE", "Room Inspection",
                s.getInspectionDate() != null ? s.getInspectionDate().atStartOfDay().atOffset(java.time.ZoneOffset.UTC) : null,
                "INSPECTION_DONE".equals(currentStatus)));

        timeline.add(new SettlementTimelineResponse("APPROVED", "Both Parties Approved",
                s.getApprovedByTenantAt() != null && s.getApprovedByHostAt() != null
                        ? s.getApprovedByTenantAt() : null,
                "APPROVED".equals(currentStatus) || "ADMIN_RESOLVED".equals(currentStatus)));

        timeline.add(new SettlementTimelineResponse("REFUND_PROCESSING", "Refund Processing",
                "REFUND_PROCESSING".equals(currentStatus) || "SETTLED".equals(currentStatus)
                        ? s.getUpdatedAt() : null,
                "REFUND_PROCESSING".equals(currentStatus)));

        timeline.add(new SettlementTimelineResponse("SETTLED", "Refund Completed",
                s.getRefundedAt(), "SETTLED".equals(currentStatus)));

        return timeline;
    }

    // ── Admin Queries ──────────────────────────────────────────

    public Page<TenancySettlement> getAllSettlements(Pageable pageable) {
        return settlementRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    public Page<TenancySettlement> getSettlementsByStatus(List<SettlementStatus> statuses, Pageable pageable) {
        return settlementRepository.findByStatusInOrderByCreatedAtDesc(statuses, pageable);
    }

    public Page<TenancySettlement> getOverdueSettlements(Pageable pageable) {
        return settlementRepository.findByIsOverdueTrueOrderByRefundDeadlineDateAsc(pageable);
    }

    public Map<String, Object> getSettlementStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("pending", settlementRepository.countByStatus(SettlementStatus.INITIATED)
                + settlementRepository.countByStatus(SettlementStatus.INSPECTION_DONE));
        stats.put("disputed", settlementRepository.countByStatus(SettlementStatus.DISPUTED));
        stats.put("overdue", settlementRepository.countByIsOverdueTrue());
        stats.put("processing", settlementRepository.countByStatus(SettlementStatus.REFUND_PROCESSING));
        stats.put("settled", settlementRepository.countByStatus(SettlementStatus.SETTLED));
        return stats;
    }

    // ── Lookups & helpers ──────────────────────────────────────

    public TenancySettlement getByTenancyId(UUID tenancyId) {
        return settlementRepository.findByTenancyId(tenancyId)
                .orElseThrow(() -> new RuntimeException("No settlement found for tenancy: " + tenancyId));
    }

    public TenancySettlement getById(UUID settlementId) {
        return settlementRepository.findById(settlementId)
                .orElseThrow(() -> new RuntimeException("Settlement not found: " + settlementId));
    }

    public SettlementResponse toResponse(TenancySettlement s) {
        List<DeductionLineResponse> deductionLines = deductionRepository.findBySettlementId(s.getId())
                .stream()
                .map(d -> new DeductionLineResponse(
                        d.getId(), d.getCategory().name(), d.getDescription(),
                        d.getAmountPaise(), d.getEvidenceUrl(),
                        d.isDisputed(), d.getDisputeReason(), d.isDisputeResolved(),
                        d.getAdminDecision(), d.getAdminAdjustedPaise(), d.getAdminNotes()))
                .toList();

        List<InspectionChecklistItemResponse> checklist = checklistRepository
                .findBySettlementIdOrderByCreatedAtAsc(s.getId())
                .stream().map(this::toChecklistResponse).toList();

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
                s.getRefundDeadlineDate(), s.getRefundDeadlineDays(),
                s.isOverdue(), s.getDisputeReason(), s.getDisputeRaisedAt(),
                s.getAdminOverrideNotes(), s.getTenantUpiId(), s.getRefundProofUrl(),
                checklist,
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

    private InspectionChecklistItemResponse toChecklistResponse(InspectionChecklistItem item) {
        return new InspectionChecklistItemResponse(
                item.getId(), item.getArea(), item.getItemName(),
                item.getCondition(), item.getDamageDescription(),
                item.getPhotoUrls(), item.getDeductionPaise(), item.getCreatedAt());
    }
}
