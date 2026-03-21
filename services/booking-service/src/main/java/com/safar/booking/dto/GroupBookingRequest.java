package com.safar.booking.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record GroupBookingRequest(
        @NotNull UUID groupId,
        @NotNull List<UUID> listingIds,
        @NotNull LocalDateTime checkIn,
        @NotNull LocalDateTime checkOut,
        @NotNull @Min(1) Integer guestsPerRoom
) {}
