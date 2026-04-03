package com.safar.booking.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecordUtilityReadingRequest(
        String utilityType,
        LocalDate readingDate,
        String meterNumber,
        BigDecimal previousReading,
        BigDecimal currentReading,
        long ratePerUnitPaise,
        String photoUrl
) {}
