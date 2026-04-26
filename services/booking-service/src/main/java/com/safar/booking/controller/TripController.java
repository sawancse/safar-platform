package com.safar.booking.controller;

import com.safar.booking.entity.Trip;
import com.safar.booking.entity.TripLeg;
import com.safar.booking.entity.enums.LegType;
import com.safar.booking.service.TripIntentEvaluator;
import com.safar.booking.service.TripService;
import com.safar.booking.service.UserClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * REST API for the Universal Trip + cross-vertical suggestion engine.
 *
 * Used primarily by safar-web's "Complete your trip" hub on the flight
 * booking confirmation page, and by other services (chef, listing, etc.)
 * to attach their bookings to an existing Trip when the user adds
 * cross-vertical legs.
 */
@RestController
@RequestMapping("/api/v1/trips")
@RequiredArgsConstructor
@Slf4j
public class TripController {

    private final TripService tripService;
    private final TripIntentEvaluator intentEvaluator;
    private final UserClient userClient;

    @GetMapping("/{id}")
    public ResponseEntity<Trip> getTrip(@PathVariable UUID id) {
        return ResponseEntity.ok(tripService.findById(id));
    }

    /**
     * Lookup the Trip a flight booking belongs to. Returns 404 if no Trip
     * has been auto-created yet (e.g. Kafka event hasn't been processed).
     * The frontend retries this for ~10s after booking before falling back
     * to the no-trip state.
     */
    @GetMapping("/by-flight-booking/{bookingId}")
    public ResponseEntity<Trip> getByFlightBooking(@PathVariable UUID bookingId) {
        return tripService.findByExternalBooking("flight-service", bookingId)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new NoSuchElementException("No trip found for flight booking: " + bookingId));
    }

    /**
     * Returns the suggested verticals for a Trip — the heart of the
     * cross-vertical "Complete your trip" hub UI.
     */
    @GetMapping("/{id}/suggestions")
    public ResponseEntity<SuggestionsResponse> getSuggestions(@PathVariable UUID id) {
        Trip trip = tripService.findById(id);
        // Fetch user flags so MEDICAL/HISTORY rules can fire — empty Set today
        // (user-service /flags endpoint not yet built); MEDICAL rules will
        // light up automatically when it ships.
        var userFlags = userClient.getUserFlags(trip.getUserId());
        TripIntentEvaluator.Result result = intentEvaluator.evaluateForTrip(trip, userFlags);
        return ResponseEntity.ok(new SuggestionsResponse(
                trip.getId(),
                result.intent().name(),
                result.suggestedVerticals().stream().map(Enum::name).toList(),
                result.matchedRuleNames()
        ));
    }

    @GetMapping("/my")
    public ResponseEntity<Page<Trip>> getMyTrips(
            Authentication auth,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(tripService.findByUserId(userId, pageable));
    }

    @PostMapping("/{id}/legs")
    public ResponseEntity<TripLeg> attachLeg(
            @PathVariable UUID id,
            @RequestBody AttachLegRequest body) {
        TripLeg leg = tripService.attachLeg(
                id, body.legType(), body.externalBookingId(), body.externalService(),
                body.amountPaise(), body.currency(), body.legOrder());
        return ResponseEntity.status(HttpStatus.CREATED).body(leg);
    }

    @DeleteMapping("/{id}/legs/{legId}")
    public ResponseEntity<Void> cancelLeg(
            @PathVariable UUID id,
            @PathVariable UUID legId,
            @RequestParam(required = false) String reason) {
        tripService.cancelLeg(id, legId, reason != null ? reason : "User cancelled leg");
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelTrip(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason) {
        tripService.cancelTrip(id, reason != null ? reason : "User cancelled trip");
        return ResponseEntity.noContent().build();
    }

    // ── DTOs ─────────────────────────────────────────────────────

    public record AttachLegRequest(
            LegType legType,
            UUID externalBookingId,
            String externalService,
            Long amountPaise,
            String currency,
            Integer legOrder
    ) {}

    public record SuggestionsResponse(
            UUID tripId,
            String intent,
            List<String> suggestedVerticals,
            List<String> matchedRules
    ) {}
}
