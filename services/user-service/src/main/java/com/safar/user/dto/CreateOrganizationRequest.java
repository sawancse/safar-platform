package com.safar.user.dto;

public record CreateOrganizationRequest(
        String name,
        String type,
        String unhcrPartnerCode,
        String contactEmail,
        String contactPhone,
        Long budgetPaise
) {}
