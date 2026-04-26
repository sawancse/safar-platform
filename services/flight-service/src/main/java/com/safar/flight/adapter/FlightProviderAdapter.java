package com.safar.flight.adapter;

import com.safar.flight.dto.CreateFlightBookingRequest;
import com.safar.flight.dto.FlightSearchRequest;
import com.safar.flight.dto.FlightSearchResponse.FlightOffer;

import java.util.List;
import java.util.UUID;

/**
 * Pluggable flight provider. Implementations: Amadeus (bookable, GDS),
 * Skyscanner (search-only, redirect-out affiliate), Kiwi/TripJack/TBO (TBD).
 *
 * Adapters return offers with provider-prefixed IDs so {@code book()} can
 * route back to the correct adapter. Affiliate adapters return REDIRECT
 * offers and throw {@link UnsupportedOperationException} on book/cancel.
 */
public interface FlightProviderAdapter {

    FlightProvider providerType();

    /** Whether this adapter is configured (creds present) and turned on. */
    boolean isEnabled();

    /** True if this adapter can issue PNRs; false for redirect-only affiliates. */
    boolean canBook();

    /**
     * Search for offers. Implementations MUST stamp each offer's
     * {@code offerId} with their provider prefix (e.g. {@code "AMADEUS:abc"})
     * and set {@code provider} + {@code bookingMode} + {@code deeplinkUrl}.
     */
    List<FlightOffer> search(FlightSearchRequest request);

    /**
     * Issue a PNR. Throws {@link UnsupportedOperationException} for
     * REDIRECT-only providers.
     */
    default ProviderBookingResult book(String providerOfferId,
                                       CreateFlightBookingRequest request,
                                       UUID userId) {
        throw new UnsupportedOperationException(
                providerType() + " is a redirect-only provider; users must book on the partner site.");
    }

    /** Cancel at the provider. No-op for affiliates (we never held the PNR). */
    default void cancel(String externalOrderId) {
        // default: no-op
    }
}
