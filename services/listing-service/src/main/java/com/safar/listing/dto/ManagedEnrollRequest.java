package com.safar.listing.dto;

import java.time.LocalDate;

public record ManagedEnrollRequest(
        Integer managementFeePct,
        LocalDate contractStart,
        Boolean autoPricing,
        Boolean autoCleaning,
        Boolean guestScreening
) {}
