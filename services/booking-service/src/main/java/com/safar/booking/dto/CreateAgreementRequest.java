package com.safar.booking.dto;

import jakarta.validation.constraints.Pattern;

public record CreateAgreementRequest(
        String tenantName,
        @Pattern(regexp = "^(\\+?91)?[6-9]\\d{9}$",
                message = "Tenant phone must be a 10-digit Indian mobile number")
        String tenantPhone,
        String tenantEmail,
        String tenantAadhaarLast4,
        String hostName,
        @Pattern(regexp = "^(\\+?91)?[6-9]\\d{9}$",
                message = "Host phone must be a 10-digit Indian mobile number")
        String hostPhone,
        String propertyAddress,
        String roomDescription,
        Integer lockInPeriodMonths,
        Long maintenanceChargesPaise,
        String termsAndConditions
) {}
