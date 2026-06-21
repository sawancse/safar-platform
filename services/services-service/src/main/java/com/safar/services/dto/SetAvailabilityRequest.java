package com.safar.services.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Vendor sets a status for a batch of dates. {@code status} is AVAILABLE,
 * BLACKOUT or HIGH_DEMAND — BOOKED is system-managed and cannot be set here.
 */
public record SetAvailabilityRequest(
        List<LocalDate> dates,
        String status,
        String notes
) {}
