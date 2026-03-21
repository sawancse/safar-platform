package com.safar.booking.dto;

import com.safar.booking.entity.enums.RecurringFrequency;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record CreateRecurringRequest(
        @NotNull UUID listingId,
        @NotNull RecurringFrequency frequency,
        @NotNull LocalTime checkInTime,
        @NotNull LocalTime checkOutTime,
        Integer dayOfWeek,
        @NotNull LocalDate startDate,
        LocalDate endDate
) {}
