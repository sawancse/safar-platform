package com.safar.user.dto;

public record UpdateKycBusinessRequest(
        String gstin,
        String businessName,
        String businessType
) {}
