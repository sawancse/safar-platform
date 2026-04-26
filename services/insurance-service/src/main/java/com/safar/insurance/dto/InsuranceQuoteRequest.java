package com.safar.insurance.dto;

import com.safar.insurance.entity.enums.CoverageType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record InsuranceQuoteRequest(
        String tripOriginCode,
        String tripDestinationCode,
        String tripOriginCountry,
        String tripDestinationCountry,

        @NotNull
        @Future
        LocalDate tripStartDate,

        @NotNull
        LocalDate tripEndDate,

        @NotNull
        CoverageType coverageType,

        @NotEmpty
        List<Integer> travellerAges
) {}
