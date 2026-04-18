package com.safar.booking.entity.enums;

public enum TenancyStatus {
    UPCOMING,      // Booking confirmed, bed reserved, tenant hasn't moved in yet
    ACTIVE,        // Tenant has moved in (check-in performed)
    NOTICE_PERIOD, // Tenant gave notice, serving out move-out window
    VACATED,       // Tenant moved out
    TERMINATED     // Cancelled before move-in (booking cancelled) or other terminal
}
