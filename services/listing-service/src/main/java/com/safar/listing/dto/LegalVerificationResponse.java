package com.safar.listing.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record LegalVerificationResponse(
        UUID id,
        UUID caseId,
        String verificationType,
        String status,
        String findings,
        String verifiedBy,
        Boolean flagged,
        String flagReason,
        OffsetDateTime verifiedAt,
        OffsetDateTime createdAt
) {}
