package com.safar.booking.dto;

public record InspectionChecklistItemDto(
        String area,
        String itemName,
        String condition,
        String damageDescription,
        String photoUrls,
        long deductionPaise
) {}
