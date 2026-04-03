package com.safar.listing.dto;

import com.safar.listing.entity.enums.ProjectStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreateBuilderProjectRequest(
        @NotBlank String builderName,
        String builderLogoUrl,
        @NotBlank String projectName,
        String tagline,
        String description,
        String reraId,
        @NotBlank String city,
        @NotBlank String state,
        String locality,
        @NotBlank String pincode,
        BigDecimal lat,
        BigDecimal lng,
        String address,
        Integer totalUnits,
        Integer totalTowers,
        Integer totalFloorsMax,
        @NotNull ProjectStatus projectStatus,
        LocalDate launchDate,
        LocalDate possessionDate,
        Integer constructionProgressPercent,
        Integer landAreaSqft,
        Integer projectAreaSqft,
        List<String> amenities,
        String masterPlanUrl,
        String brochureUrl,
        String walkthroughUrl,
        List<String> photos,
        List<String> bankApprovals,
        String paymentPlansJson
) {}
