package com.safar.listing.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.time.LocalTime;

public record SessionRequest(
        @NotNull LocalDate sessionDate,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        @Positive int availableSpots
) {}
