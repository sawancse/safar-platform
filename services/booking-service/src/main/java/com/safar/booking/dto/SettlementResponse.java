package com.safar.booking.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record SettlementResponse(
        UUID id,
        UUID tenancyId,
        String settlementRef,
        LocalDate moveOutDate,
        LocalDate inspectionDate,
        String inspectionNotes,
        long securityDepositPaise,
        long unpaidRentPaise,
        long unpaidUtilitiesPaise,
        long damageDeductionPaise,
        long latePenaltyPaise,
        long otherDeductionsPaise,
        String otherDeductionsNote,
        long totalDeductionsPaise,
        long refundAmountPaise,
        long additionalDuePaise,
        String status,
        OffsetDateTime approvedByHostAt,
        OffsetDateTime approvedByTenantAt,
        String razorpayRefundId,
        List<DeductionLineResponse> deductions,
        String settlementPdfUrl,
        OffsetDateTime createdAt
) {}
