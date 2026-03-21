package com.safar.listing.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record RoomTypeAvailabilityRequest(
        @NotNull LocalDate fromDate,
        @NotNull LocalDate toDate,
        @NotNull @Min(1) Integer count
) {}
