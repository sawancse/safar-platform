package com.safar.listing.dto;

import java.util.UUID;

public record CreateLegalCaseRequest(
        String packageType,
        UUID propertyId,
        String propertyAddress,
        String propertyCity,
        String propertyState,
        String surveyNumber
) {}
