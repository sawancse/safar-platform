package com.safar.user.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CohostAgreementRequest(
        @NotNull Integer feePct,
        @NotNull String services,
        @NotNull LocalDate startDate
) {}
