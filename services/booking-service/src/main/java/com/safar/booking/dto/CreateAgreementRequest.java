package com.safar.booking.dto;

public record CreateAgreementRequest(
        String tenantName,
        String tenantPhone,
        String tenantEmail,
        String tenantAadhaarLast4,
        String hostName,
        String hostPhone,
        String propertyAddress,
        String roomDescription,
        Integer lockInPeriodMonths,
        Long maintenanceChargesPaise,
        String termsAndConditions
) {}
