package com.safar.listing.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AdvocateResponse(
        UUID id,
        String fullName,
        String barCouncilId,
        String phone,
        String email,
        String city,
        String state,
        String photoUrl,
        Integer experienceYears,
        List<String> specializations,
        BigDecimal rating,
        Integer casesCompleted,
        Long consultationFeePaise,
        Boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
