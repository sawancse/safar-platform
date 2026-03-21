package com.safar.listing.dto;

import com.safar.listing.entity.enums.RwCertStatus;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

public record RwCertResponse(
        UUID id,
        UUID listingId,
        RwCertStatus status,
        Integer wifiSpeedMbps,
        Boolean hasDedicatedDesk,
        Boolean hasPowerBackup,
        LocalTime quietHoursFrom,
        LocalTime quietHoursTo,
        String additionalNotes,
        OffsetDateTime certifiedAt,
        OffsetDateTime expiresAt,
        OffsetDateTime submittedAt,
        String adminNote
) {}
