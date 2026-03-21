package com.safar.user.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record MedicalProfileResponse(
    UUID id,
    UUID userId,
    String bloodGroup,
    String allergies,
    String currentMedications,
    String pastSurgeries,
    String chronicConditions,
    String emergencyContactName,
    String emergencyContactPhone,
    String emergencyContactRelation,
    String preferredLanguage,
    String dietaryRestrictions,
    String mobilityNeeds,
    String insuranceProvider,
    String insurancePolicyNumber,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
