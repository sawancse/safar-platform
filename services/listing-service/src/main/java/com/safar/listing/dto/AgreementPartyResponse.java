package com.safar.listing.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AgreementPartyResponse(
        UUID id,
        UUID agreementId,
        String partyType,
        String fullName,
        String aadhaarNumber,
        String panNumber,
        String address,
        String phone,
        String email,
        Boolean verified,
        OffsetDateTime createdAt
) {}
