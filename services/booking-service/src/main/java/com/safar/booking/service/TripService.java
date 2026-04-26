package com.safar.booking.service;

import com.safar.booking.entity.Trip;
import com.safar.booking.entity.TripLeg;
import com.safar.booking.entity.enums.LegStatus;
import com.safar.booking.entity.enums.LegType;
import com.safar.booking.entity.enums.TripIntent;
import com.safar.booking.entity.enums.TripStatus;
import com.safar.booking.repository.TripLegRepository;
import com.safar.booking.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

/**
 * CRUD + leg-attach + cancel orchestration for Universal Trips.
 *
 * Cancel rules (per Tree-3 of the design doc):
 *  - cancel-one-leg: leg → CANCELLED, Trip → PARTIAL_CANCEL, no auto-cascade
 *  - cancel-trip: cascade-cancel all legs (caller must trigger provider-side
 *    cancels via their respective services); Trip → CANCELLED
 *  - airline-initiated cancel: same as user cancel-one-leg flow
 *  - bundle discount NOT clawed back on partial cancel
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TripService {

    private final TripRepository tripRepository;
    private final TripLegRepository tripLegRepository;

    @Transactional
    public Trip create(UUID userId, String tripName,
                       String originCity, String destinationCity,
                       String originCode, String destinationCode,
                       String originCountry, String destinationCountry,
                       LocalDate startDate, LocalDate endDate,
                       Integer paxCount, TripIntent intent) {
        Trip trip = Trip.builder()
                .userId(userId)
                .tripName(tripName != null ? tripName : defaultTripName(originCity, destinationCity, startDate))
                .originCity(originCity)
                .destinationCity(destinationCity)
                .originCode(originCode)
                .destinationCode(destinationCode)
                .originCountry(originCountry != null ? originCountry : "IN")
                .destinationCountry(destinationCountry != null ? destinationCountry : "IN")
                .startDate(startDate)
                .endDate(endDate)
                .paxCount(paxCount != null ? paxCount : 1)
                .tripIntent(intent != null ? intent : TripIntent.UNCLASSIFIED)
                .status(TripStatus.DRAFT)
                .build();
        trip = tripRepository.save(trip);
        log.info("Trip created: {} for user {} ({} → {} on {})",
                trip.getId(), userId, originCity, destinationCity, startDate);
        return trip;
    }

    /**
     * Convenience: create a Trip seeded from a flight-booking Kafka payload
     * + attach the FLIGHT leg in one transaction. Called by
     * FlightBookingCreatedConsumer.
     */
    @Transactional
    public Trip createFromFlightBooking(UUID userId, UUID flightBookingId,
                                        String originCity, String originCityCode,
                                        String destinationCity, String destinationCityCode,
                                        LocalDate departureDate, LocalDate returnDate,
                                        Integer paxCount, Long totalAmountPaise,
                                        String currency, Boolean isInternational) {
        // Idempotency — if we've already attached this booking to a Trip,
        // just return the existing one (Kafka events can replay).
        Optional<TripLeg> existing = tripLegRepository
                .findByExternalServiceAndExternalBookingId("flight-service", flightBookingId);
        if (existing.isPresent()) {
            log.info("Flight booking {} already attached to Trip {}; skipping",
                    flightBookingId, existing.get().getTrip().getId());
            return existing.get().getTrip();
        }

        String originCountry = isInternational != null && isInternational ? null : "IN";
        String destinationCountry = isInternational != null && isInternational ? null : "IN";

        // End date defaults to return date if present, else departure date (one-way).
        LocalDate endDate = returnDate != null ? returnDate : departureDate;
        String tripName = defaultTripName(
                originCity != null ? originCity : originCityCode,
                destinationCity != null ? destinationCity : destinationCityCode,
                departureDate);

        Trip trip = create(userId, tripName,
                originCity != null ? originCity : originCityCode,
                destinationCity != null ? destinationCity : destinationCityCode,
                originCityCode, destinationCityCode,
                originCountry, destinationCountry,
                departureDate, endDate, paxCount, null);

        attachLeg(trip.getId(), LegType.FLIGHT, flightBookingId, "flight-service",
                totalAmountPaise, currency, 1);
        // Trip moves to CONFIRMED once first leg is attached & confirmed.
        trip.setStatus(TripStatus.CONFIRMED);
        return tripRepository.save(trip);
    }

    @Transactional
    public TripLeg attachLeg(UUID tripId, LegType legType, UUID externalBookingId,
                             String externalService, Long amountPaise, String currency, Integer legOrder) {
        Trip trip = requireTrip(tripId);
        TripLeg leg = TripLeg.builder()
                .trip(trip)
                .legType(legType)
                .externalBookingId(externalBookingId)
                .externalService(externalService)
                .status(LegStatus.CONFIRMED)
                .legOrder(legOrder != null ? legOrder : trip.getLegs().size() + 1)
                .amountPaise(amountPaise)
                .currency(currency != null ? currency : "INR")
                .build();
        leg = tripLegRepository.save(leg);
        log.info("Attached {} leg {} to Trip {}", legType, leg.getId(), tripId);
        return leg;
    }

    @Transactional
    public void cancelLeg(UUID tripId, UUID legId, String reason) {
        Trip trip = requireTrip(tripId);
        TripLeg leg = trip.getLegs().stream()
                .filter(l -> l.getId().equals(legId))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Leg not found in trip: " + legId));

        leg.setStatus(LegStatus.CANCELLED);
        leg.setCancelledAt(Instant.now());
        leg.setCancellationReason(reason);
        tripLegRepository.save(leg);

        // Trip → PARTIAL_CANCEL if at least one leg remains active; CANCELLED if all gone.
        boolean anyActive = trip.getLegs().stream()
                .anyMatch(l -> l.getStatus() == LegStatus.CONFIRMED || l.getStatus() == LegStatus.PENDING);
        trip.setStatus(anyActive ? TripStatus.PARTIAL_CANCEL : TripStatus.CANCELLED);
        tripRepository.save(trip);
        log.info("Cancelled leg {} of Trip {}; trip status={}", legId, tripId, trip.getStatus());
    }

    @Transactional
    public void cancelTrip(UUID tripId, String reason) {
        Trip trip = requireTrip(tripId);
        Instant now = Instant.now();
        for (TripLeg leg : trip.getLegs()) {
            if (leg.getStatus() == LegStatus.CONFIRMED || leg.getStatus() == LegStatus.PENDING) {
                leg.setStatus(LegStatus.CANCELLED);
                leg.setCancelledAt(now);
                leg.setCancellationReason(reason);
            }
        }
        tripLegRepository.saveAll(trip.getLegs());
        trip.setStatus(TripStatus.CANCELLED);
        tripRepository.save(trip);
        log.info("Cancelled Trip {} with {} legs", tripId, trip.getLegs().size());
    }

    public Trip findById(UUID tripId) {
        return requireTrip(tripId);
    }

    public Optional<Trip> findByExternalBooking(String externalService, UUID externalBookingId) {
        return tripLegRepository
                .findByExternalServiceAndExternalBookingId(externalService, externalBookingId)
                .map(TripLeg::getTrip);
    }

    public Page<Trip> findByUserId(UUID userId, Pageable pageable) {
        return tripRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    // ── helpers ─────────────────────────────────────────────────

    private Trip requireTrip(UUID tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new NoSuchElementException("Trip not found: " + tripId));
    }

    private static String defaultTripName(String origin, String dest, LocalDate date) {
        StringBuilder sb = new StringBuilder();
        if (origin != null) sb.append(origin);
        sb.append(" → ");
        if (dest != null) sb.append(dest);
        if (date != null) sb.append(", ").append(date);
        return sb.toString();
    }
}
