package com.safar.booking.entity.enums;

public enum TripStatus {
    DRAFT,            // trip object exists; no leg confirmed yet
    CONFIRMED,        // all legs confirmed
    PARTIAL_CANCEL,   // 1+ legs cancelled, others still active
    IN_PROGRESS,      // start_date reached, trip ongoing
    COMPLETED,        // end_date passed, all legs in terminal state
    CANCELLED         // entire trip cancelled (cascade or user-initiated)
}
