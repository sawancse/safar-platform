package com.safar.services.dto;

import com.safar.services.entity.ServiceListingAvailability;

import java.time.LocalDate;
import java.util.UUID;

public record AvailabilityResponse(
        UUID id,
        LocalDate date,
        String startTime,
        String endTime,
        String status,
        UUID bookingId,
        String notes
) {
    public static AvailabilityResponse from(ServiceListingAvailability a) {
        return new AvailabilityResponse(
                a.getId(),
                a.getDate(),
                a.getStartTime() != null ? a.getStartTime().toString() : null,
                a.getEndTime() != null ? a.getEndTime().toString() : null,
                a.getStatus(),
                a.getBookingId(),
                a.getNotes());
    }
}
