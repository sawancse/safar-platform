package com.safar.flight.repository;

import com.safar.flight.entity.FlightSearchEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface FlightSearchEventRepository extends JpaRepository<FlightSearchEvent, UUID> {

    /**
     * Detector hot path. Find unsuppressed events whose age falls within a
     * pulse window — i.e. created between {@code maxAge} (e.g. 7 hrs ago)
     * and {@code minAge} (e.g. 5 hrs ago) for the 6-hr pulse, and which
     * haven't already had {@code expectedReminders} reminders sent.
     */
    @Query("""
            SELECT e FROM FlightSearchEvent e
             WHERE e.suppressed = false
               AND e.remindersSent = :expectedReminders
               AND e.createdAt BETWEEN :maxAge AND :minAge
               AND e.departureDate >= :today
            """)
    List<FlightSearchEvent> findCandidatesForPulse(
            @Param("expectedReminders") int expectedReminders,
            @Param("maxAge") Instant maxAge,
            @Param("minAge") Instant minAge,
            @Param("today") LocalDate today);

    /**
     * Booking-side suppression. When a flight is booked, mark all of this
     * user's matching open searches as suppressed=BOOKED so we stop nudging.
     */
    List<FlightSearchEvent> findByUserIdAndOriginAndDestinationAndDepartureDateAndSuppressedFalse(
            UUID userId, String origin, String destination, LocalDate departureDate);

    /**
     * Anonymous → identified promotion. When a user logs in, attach their
     * recent device-keyed searches to the now-known user_id so reminders
     * can be delivered via email/WA (not just push).
     */
    List<FlightSearchEvent> findByDeviceIdAndUserIdIsNullAndCreatedAtAfter(
            String deviceId, Instant since);
}
