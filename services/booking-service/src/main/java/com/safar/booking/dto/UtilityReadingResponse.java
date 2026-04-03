package com.safar.booking.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record UtilityReadingResponse(
        UUID id,
        UUID tenancyId,
        String utilityType,
        String meterNumber,
        LocalDate readingDate,
        BigDecimal previousReading,
        BigDecimal currentReading,
        BigDecimal unitsConsumed,
        long ratePerUnitPaise,
        long totalChargePaise,
        Integer billingMonth,
        Integer billingYear,
        UUID invoiceId,
        String photoUrl
) {}
