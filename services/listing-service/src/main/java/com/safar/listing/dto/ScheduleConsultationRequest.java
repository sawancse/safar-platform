package com.safar.listing.dto;

import java.time.LocalDateTime;

public record ScheduleConsultationRequest(
        LocalDateTime scheduledAt,
        Integer durationMinutes
) {}
