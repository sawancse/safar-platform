package com.safar.listing.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record HospitalProcedureDto(
        UUID id,
        UUID hospitalId,
        String procedureName,
        String specialty,
        Long estCostMinPaise,
        Long estCostMaxPaise,
        Integer hospitalDays,
        Integer recoveryDays,
        BigDecimal successRate,
        String description
) {}
