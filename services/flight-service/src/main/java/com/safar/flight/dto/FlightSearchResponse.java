package com.safar.flight.dto;

import java.util.List;

public record FlightSearchResponse(
        List<FlightOffer> offers
) {
    public record FlightOffer(
            String offerId,
            String airline,
            String airlineLogo,
            String flightNumber,
            String departureTime,
            String arrivalTime,
            String duration,
            int stops,
            long pricePaise,
            String currency,
            String cabinClass,
            List<Segment> segments
    ) {}

    public record Segment(
            String segmentId,
            String airline,
            String flightNumber,
            String originCode,
            String originCity,
            String destinationCode,
            String destinationCity,
            String departureTime,
            String arrivalTime,
            String duration,
            String aircraft
    ) {}
}
