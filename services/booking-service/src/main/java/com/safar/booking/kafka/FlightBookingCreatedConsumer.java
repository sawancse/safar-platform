package com.safar.booking.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safar.booking.service.TripService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Auto-creates a Universal Trip + attaches the flight as the first leg
 * whenever flight-service publishes a {@code flight.booking.created} event.
 *
 * This is what makes the cross-vertical engine actually fire — without
 * this consumer, FlightBookings exist but no Trip object ever gets
 * created, so the "Complete your trip" hub UI has nothing to display.
 *
 * Idempotent — replaying a Kafka event won't create a duplicate Trip
 * (TripService.createFromFlightBooking checks for existing leg first).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FlightBookingCreatedConsumer {

    private final TripService tripService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "flight.booking.created", groupId = "booking-service-trip")
    public void onFlightBookingCreated(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);

            String userIdStr = node.path("userId").asText("");
            String bookingIdStr = node.path("bookingId").asText("");
            if (userIdStr.isBlank() || bookingIdStr.isBlank()) {
                log.warn("Skipping flight.booking.created with missing userId/bookingId: {}", message);
                return;
            }

            UUID userId = UUID.fromString(userIdStr);
            UUID flightBookingId = UUID.fromString(bookingIdStr);

            String originCity = node.path("departureCity").asText("");
            String originCityCode = node.path("departureCityCode").asText("");
            String destinationCity = node.path("arrivalCity").asText("");
            String destinationCityCode = node.path("arrivalCityCode").asText("");

            String departureDateStr = node.path("departureDate").asText("");
            String returnDateStr = node.path("returnDate").asText("");
            LocalDate departureDate = !departureDateStr.isBlank() ? LocalDate.parse(departureDateStr) : null;
            LocalDate returnDate = !returnDateStr.isBlank() ? LocalDate.parse(returnDateStr) : null;

            // Pax count is not on the event payload today; default to 1.
            // TODO: extend flight-service publishEvent() with passengerCount to drive better Trip Intent matching
            Integer paxCount = 1;

            Long totalAmountPaise = node.path("totalAmountPaise").asLong(0);
            String currency = node.path("currency").asText("INR");
            Boolean isInternational = node.path("isInternational").asBoolean(false);

            tripService.createFromFlightBooking(
                    userId, flightBookingId,
                    originCity, originCityCode,
                    destinationCity, destinationCityCode,
                    departureDate, returnDate,
                    paxCount, totalAmountPaise, currency, isInternational);

        } catch (Exception e) {
            log.error("Failed to create Trip from flight.booking.created event: {}", e.getMessage(), e);
        }
    }
}
