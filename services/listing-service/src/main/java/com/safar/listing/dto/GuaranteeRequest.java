package com.safar.listing.dto;

import java.time.LocalDate;

public record GuaranteeRequest(
        Long monthlyGuaranteePaise,
        LocalDate contractStart,
        Integer durationMonths
) {}
