package com.safar.listing.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PartnerBankResponse(
        UUID id,
        String bankName,
        String bankCode,
        String logoUrl,
        BigDecimal minInterestRate,
        BigDecimal maxInterestRate,
        Long maxLoanAmountPaise,
        Integer maxTenureMonths,
        BigDecimal processingFeePercent,
        Long processingFeeCapPaise,
        Boolean preApprovalAvailable,
        Boolean balanceTransferAvailable,
        Boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
