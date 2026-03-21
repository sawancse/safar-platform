package com.safar.user.dto;

public record MedicalProfileRequest(
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
    String insurancePolicyNumber
) {}
