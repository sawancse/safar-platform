package com.safar.booking.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AgreementResponse(
        UUID id,
        UUID tenancyId,
        String agreementNumber,
        String status,
        String tenantName,
        String tenantPhone,
        String tenantEmail,
        String hostName,
        String hostPhone,
        String propertyAddress,
        String roomDescription,
        LocalDate moveInDate,
        int lockInPeriodMonths,
        int noticePeriodDays,
        long monthlyRentPaise,
        long securityDepositPaise,
        long maintenanceChargesPaise,
        long stampDutyPaise,
        OffsetDateTime hostSignedAt,
        OffsetDateTime tenantSignedAt,
        String agreementPdfUrl,
        OffsetDateTime createdAt
) {}
