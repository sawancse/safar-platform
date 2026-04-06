package com.safar.listing.dto;

import com.safar.listing.entity.enums.LoanApplicationStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record LoanApplicationResponse(
        UUID id,
        UUID userId,
        UUID bankId,
        String bankName,
        String bankLogoUrl,
        Long loanAmountPaise,
        Integer tenureMonths,
        BigDecimal interestRate,
        Long emiPaise,
        Long sanctionedAmountPaise,
        UUID propertyId,
        String applicantName,
        String applicantPhone,
        String applicantEmail,
        LoanApplicationStatus status,
        String remarks,
        String referenceNumber,
        OffsetDateTime appliedAt,
        OffsetDateTime sanctionedAt,
        OffsetDateTime disbursedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
