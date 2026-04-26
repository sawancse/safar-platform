package com.safar.booking.entity.enums;

/**
 * Inferred at trip creation from route + dates + group composition + user
 * profile. User can override. Drives the cross-vertical suggestion engine
 * (which verticals to surface; e.g. PILGRIMAGE → suggest pandit + sattvik cook).
 */
public enum TripIntent {
    PILGRIMAGE,
    WEDDING,
    BUSINESS,
    LEISURE,
    MOVE_IN,
    MEDICAL,
    FAMILY,
    EDUCATION,
    UNCLASSIFIED
}
