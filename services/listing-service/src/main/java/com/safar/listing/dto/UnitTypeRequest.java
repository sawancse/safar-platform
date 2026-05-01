package com.safar.listing.dto;

import com.safar.listing.entity.enums.FurnishingStatus;
import com.safar.listing.entity.enums.UnitKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.util.List;

/**
 * For UNIT (apartment/villa): bhk + carpet area required, plot fields ignored.
 * For PLOT (plotted dev): plotAreaSqft + pricePerSqftPaise required, bhk
 * optional. Service layer enforces the per-kind validation since this DTO
 * accepts both shapes.
 */
public record UnitTypeRequest(
        @NotBlank String name,
        UnitKind unitKind,            // defaults to UNIT in service if null
        Integer bhk,
        Integer carpetAreaSqft,
        Integer builtUpAreaSqft,
        Integer superBuiltUpAreaSqft,
        // Pricing — basePricePaise is the canonical total. For plots the
        // frontend may send pricePerSqftPaise + plotAreaSqft and the service
        // computes basePricePaise = pricePerSqftPaise × plotAreaSqft.
        @Positive Long basePricePaise,
        Long pricePerSqftPaise,
        Long floorRisePaise,
        Long facingPremiumPaise,
        Integer premiumFloorsFrom,
        Integer totalUnits,
        Integer bathrooms,
        Integer balconies,
        FurnishingStatus furnishing,
        // Plot fields
        Integer plotAreaSqft,
        Integer plotLengthFt,
        Integer plotBreadthFt,
        Boolean cornerPlot,
        String facing,
        // Media
        String floorPlanUrl,
        String unitLayoutUrl,
        List<String> photos
) {}
