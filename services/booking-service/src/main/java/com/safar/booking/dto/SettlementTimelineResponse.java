package com.safar.booking.dto;

import java.time.OffsetDateTime;

public record SettlementTimelineResponse(
        String status,
        String label,
        OffsetDateTime timestamp,
        boolean current
) {}
