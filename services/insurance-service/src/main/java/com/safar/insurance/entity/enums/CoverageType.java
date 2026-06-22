package com.safar.insurance.entity.enums;

public enum CoverageType {
    // Travel
    DOMESTIC_TRAVEL,
    INTERNATIONAL_TRAVEL,
    STUDENT_TRAVEL,
    // Embedded (booking-attached)
    STAY_PROTECTION,        // stay cancellation / interruption
    TENANT_CONTENTS,        // renter contents + liability (PG / long-term)
    // Standalone marketplace products
    HEALTH,
    LIFE_TERM,
    MOTOR,
    HOME,
    PERSONAL_ACCIDENT
}
