package com.safar.listing.dto;

import com.safar.listing.entity.enums.FinancingType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record CreateInquiryRequest(
        UUID salePropertyId,
        UUID builderProjectId,
        String message,
        String buyerName,
        String buyerPhone,
        String buyerEmail,
        LocalDate preferredVisitDate,
        String preferredVisitTime,
        FinancingType financingType,
        Long budgetMinPaise,
        Long budgetMaxPaise
) {}
