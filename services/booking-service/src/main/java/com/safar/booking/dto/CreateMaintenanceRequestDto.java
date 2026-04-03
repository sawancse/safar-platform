package com.safar.booking.dto;

public record CreateMaintenanceRequestDto(
        String category,
        String title,
        String description,
        String photoUrls,
        String priority
) {}
