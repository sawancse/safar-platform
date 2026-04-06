package com.safar.listing.dto;

import com.safar.listing.entity.enums.AgreementStatus;
import com.safar.listing.entity.enums.AgreementType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AgreementResponse(
        UUID id,
        UUID userId,
        AgreementType agreementType,
        UUID propertyId,
        UUID listingId,
        String packageType,
        String partyDetailsJson,
        String propertyDetailsJson,
        String clausesJson,
        AgreementStatus status,
        String draftPdfUrl,
        String signedPdfUrl,
        String registeredPdfUrl,
        Long stampDutyPaise,
        Long registrationFeePaise,
        Long serviceFeePaise,
        Long totalAmountPaise,
        String rejectionReason,
        OffsetDateTime signedAt,
        OffsetDateTime registeredAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<AgreementPartyResponse> parties
) {}
