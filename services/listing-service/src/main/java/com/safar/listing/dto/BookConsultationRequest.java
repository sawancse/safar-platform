package com.safar.listing.dto;

import java.time.LocalDate;

public record BookConsultationRequest(
        String projectType,
        String propertyType,
        String propertyAddress,
        String city,
        String state,
        String pincode,
        Integer roomCount,
        Long budgetMinPaise,
        Long budgetMaxPaise,
        LocalDate consultationDate
) {}
