package com.safar.listing.dto;

import com.safar.listing.entity.enums.InteriorProjectStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record InteriorProjectResponse(
        UUID id,
        UUID userId,
        UUID designerId,
        String designerName,
        String designerPhone,
        String designerEmail,
        String projectType,
        String propertyType,
        String propertyAddress,
        String city,
        String state,
        String pincode,
        Integer roomCount,
        Long budgetMinPaise,
        Long budgetMaxPaise,
        LocalDate consultationDate,
        InteriorProjectStatus status,
        Integer roomDesignsCount,
        Long quotedAmountPaise,
        Long approvedAmountPaise,
        Long paidAmountPaise,
        String notes,
        LocalDate expectedStartDate,
        LocalDate expectedEndDate,
        LocalDate actualStartDate,
        LocalDate actualEndDate,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
