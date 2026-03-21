package com.safar.listing.dto;

import java.util.List;

public record MedicalCostEstimate(
        String procedureName,
        String specialty,
        String hospitalName,
        String city,
        Long treatmentMinPaise,
        Long treatmentMaxPaise,
        Integer hospitalDays,
        Integer recoveryDays,
        Long stayPerNightPaise,
        Integer stayNights,
        Long stayTotalPaise,
        Long totalMinPaise,
        Long totalMaxPaise,
        List<String> timeline
) {}
