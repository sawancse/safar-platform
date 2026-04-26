package com.safar.flight.adapter;

import com.safar.flight.entity.CabinClass;
import com.safar.flight.entity.TripType;

import java.time.LocalDate;

/**
 * What a provider's book() returns. The metadata fields (airline,
 * route, dates) are populated from the provider's order response and
 * persisted onto the FlightBooking row.
 */
public record ProviderBookingResult(
        String externalOrderId,
        String providerStatus,
        long totalAmountPaise,
        long taxPaise,
        String currency,

        String departureCityCode,
        String arrivalCityCode,
        LocalDate departureDate,
        LocalDate returnDate,
        TripType tripType,
        CabinClass cabinClass,
        String airline,
        String flightNumber,
        boolean isInternational,
        String itinerariesJson
) {}
