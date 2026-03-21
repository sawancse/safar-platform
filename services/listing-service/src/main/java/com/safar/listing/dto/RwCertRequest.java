package com.safar.listing.dto;

import java.time.LocalTime;

public record RwCertRequest(
        Integer wifiSpeedMbps,
        Boolean hasDedicatedDesk,
        Boolean hasPowerBackup,
        LocalTime quietHoursFrom,
        LocalTime quietHoursTo,
        String additionalNotes
) {}
