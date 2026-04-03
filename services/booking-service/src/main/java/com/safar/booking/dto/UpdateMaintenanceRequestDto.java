package com.safar.booking.dto;

public record UpdateMaintenanceRequestDto(
        String status,
        String assignedTo,
        String resolutionNotes
) {}
