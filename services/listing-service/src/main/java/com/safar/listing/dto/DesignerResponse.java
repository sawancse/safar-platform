package com.safar.listing.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record DesignerResponse(
        UUID id,
        String fullName,
        String firmName,
        String phone,
        String email,
        String city,
        String state,
        String photoUrl,
        String portfolioUrl,
        Integer experienceYears,
        List<String> specializations,
        BigDecimal rating,
        Integer projectsCompleted,
        Long consultationFeePaise,
        Boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
