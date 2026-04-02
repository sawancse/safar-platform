package com.safar.listing.dto;

import com.safar.listing.entity.enums.SessionStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record SessionResponse(
        UUID id,
        LocalDate sessionDate,
        LocalTime startTime,
        LocalTime endTime,
        int availableSpots,
        int bookedSpots,
        SessionStatus status
) {}
