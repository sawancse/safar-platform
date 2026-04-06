package com.safar.listing.dto;

import java.util.UUID;

public record CreateAgreementRequest(
        String agreementType,
        UUID propertyId,
        UUID listingId,
        String packageType,
        String partyDetailsJson,
        String propertyDetailsJson,
        String clausesJson
) {}
