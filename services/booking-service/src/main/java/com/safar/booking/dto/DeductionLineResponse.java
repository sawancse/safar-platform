package com.safar.booking.dto;

import java.util.UUID;

public record DeductionLineResponse(
        UUID id,
        String category,
        String description,
        long amountPaise,
        String evidenceUrl
) {}
