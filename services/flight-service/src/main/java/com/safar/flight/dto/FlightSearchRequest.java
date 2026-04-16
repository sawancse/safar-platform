package com.safar.flight.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record FlightSearchRequest(
        @NotBlank(message = "Origin IATA code is required")
        String origin,

        @NotBlank(message = "Destination IATA code is required")
        String destination,

        @NotNull(message = "Departure date is required")
        @Future(message = "Departure date must be in the future")
        LocalDate departureDate,

        LocalDate returnDate,

        @NotNull(message = "Number of passengers is required")
        @Min(value = 1, message = "At least 1 passenger required")
        Integer passengers,

        String cabinClass,

        Integer maxConnections
) {}
