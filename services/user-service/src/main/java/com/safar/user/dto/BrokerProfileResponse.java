package com.safar.user.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record BrokerProfileResponse(
        UUID id,
        UUID userId,
        String companyName,
        String reraAgentId,
        Boolean reraVerified,
        List<String> operatingCities,
        String specialization,
        Integer experienceYears,
        Integer totalDealsCount,
        String bio,
        String website,
        String officeAddress,
        String officeCity,
        String officeState,
        String officePincode,
        String subscriptionTier,
        Boolean verified,
        Boolean active,
        String userName,
        String userPhone,
        String userEmail,
        String avatarUrl,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
