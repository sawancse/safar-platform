package com.safar.user.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CoTravelerDto(
        UUID id,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String gender,
        OffsetDateTime createdAt
) {}
