package com.safar.review.service;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Interface for booking ownership checks.
 * Real implementation would call booking-service via HTTP.
 * Tests use a mock.
 */
public interface BookingServiceClient {

    /** Returns true if the booking is CONFIRMED and belongs to the given guest. */
    boolean isConfirmedBookingOwner(UUID bookingId, UUID guestId);

    /** Returns the host ID for the given booking. */
    UUID getHostIdForBooking(UUID bookingId);

    /** Returns the listing ID for the given booking. */
    UUID getListingIdForBooking(UUID bookingId);

    /** Returns the guest full name for the given booking. */
    String getGuestNameForBooking(UUID bookingId);

    /** Returns the checkout datetime for double-blind review deadline. */
    default OffsetDateTime getCheckOutForBooking(UUID bookingId) {
        return OffsetDateTime.now(); // fallback: 14 days from now
    }
}
