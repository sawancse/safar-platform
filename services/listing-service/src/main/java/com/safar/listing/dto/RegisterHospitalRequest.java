package com.safar.listing.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record RegisterHospitalRequest(
        @NotBlank String name,
        @NotBlank String city,
        @NotBlank String address,
        BigDecimal lat,
        BigDecimal lng,
        String specialties,
        String accreditations,
        String contactEmail
) {}
