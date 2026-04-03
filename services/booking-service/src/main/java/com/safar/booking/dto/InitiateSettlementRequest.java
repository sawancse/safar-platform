package com.safar.booking.dto;

import java.time.LocalDate;

public record InitiateSettlementRequest(
        LocalDate moveOutDate,
        LocalDate inspectionDate
) {}
