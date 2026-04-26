package com.safar.insurance.adapter;

import com.safar.insurance.entity.enums.CoverageType;

import java.time.LocalDate;
import java.util.List;

/**
 * Adapter-facing quote request. Same shape across providers; adapters
 * translate to per-provider request bodies internally.
 */
public record QuoteRequest(
        String tripOriginCode,
        String tripDestinationCode,
        String tripOriginCountry,
        String tripDestinationCountry,
        LocalDate tripStartDate,
        LocalDate tripEndDate,
        CoverageType coverageType,
        List<Integer> travellerAges    // ages of insured travellers
) {}
