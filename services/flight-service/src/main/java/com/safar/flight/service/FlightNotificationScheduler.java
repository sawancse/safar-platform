package com.safar.flight.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safar.flight.entity.FlightBooking;
import com.safar.flight.entity.FlightBookingStatus;
import com.safar.flight.repository.FlightBookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Schedulers for flight booking notifications:
 * - Check-in reminder (24h before departure)
 * - Trip reminder (1 day before)
 * - Payment expiry (30 min pending → auto-cancel)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FlightNotificationScheduler {

    private final FlightBookingRepository bookingRepository;
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper objectMapper;

    /**
     * Daily at 8 AM: Send check-in + trip reminders for flights departing tomorrow.
     * Publishes flight.reminder.checkin and flight.reminder.trip Kafka events.
     * notification-service consumes these and sends Email + SMS + In-App.
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void sendDepartureReminders() {
        log.info("Starting flight departure reminder run");
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        List<FlightBooking> departing = bookingRepository.findByDepartureDateAndStatusIn(
                tomorrow, List.of(FlightBookingStatus.CONFIRMED, FlightBookingStatus.TICKETED));

        int sent = 0;
        for (FlightBooking booking : departing) {
            try {
                Map<String, Object> event = buildNotificationEvent(booking, "DEPARTURE_REMINDER");
                event.put("reminderType", "checkin");
                event.put("message", "Web check-in is now open for your flight " + booking.getFlightNumber()
                        + " (" + booking.getDepartureCityCode() + " → " + booking.getArrivalCityCode() + ") tomorrow.");

                String json = objectMapper.writeValueAsString(event);
                kafka.send("flight.reminder.checkin", booking.getId().toString(), json);
                sent++;
            } catch (Exception e) {
                log.error("Failed to send departure reminder for {}: {}", booking.getBookingRef(), e.getMessage());
            }
        }
        log.info("Departure reminders sent for {}/{} flights", sent, departing.size());
    }

    /**
     * Every 5 min: Auto-cancel bookings stuck in PENDING_PAYMENT for > 30 minutes.
     * Publishes flight.booking.expired event.
     */
    @Scheduled(fixedRate = 300_000)
    public void expireStalePendingPayments() {
        Instant cutoff = Instant.now().minusSeconds(1800); // 30 min ago
        List<FlightBooking> stale = bookingRepository.findStalePendingPayments(cutoff);

        for (FlightBooking booking : stale) {
            booking.setStatus(FlightBookingStatus.CANCELLED);
            booking.setCancellationReason("Payment not completed within 30 minutes");
            booking.setCancelledAt(Instant.now());
            bookingRepository.save(booking);

            try {
                Map<String, Object> event = buildNotificationEvent(booking, "PAYMENT_EXPIRED");
                event.put("message", "Your flight booking " + booking.getBookingRef()
                        + " has been cancelled as payment was not completed within 30 minutes.");
                String json = objectMapper.writeValueAsString(event);
                kafka.send("flight.booking.expired", booking.getId().toString(), json);
            } catch (Exception e) {
                log.warn("Failed to publish flight.booking.expired for {}: {}", booking.getBookingRef(), e.getMessage());
            }

            log.info("Auto-cancelled stale flight booking: {} (created {})", booking.getBookingRef(), booking.getCreatedAt());
        }
    }

    private Map<String, Object> buildNotificationEvent(FlightBooking b, String eventType) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("bookingId", b.getId().toString());
        event.put("bookingRef", b.getBookingRef());
        event.put("userId", b.getUserId().toString());
        event.put("eventType", eventType);
        event.put("contactEmail", b.getContactEmail() != null ? b.getContactEmail() : "");
        event.put("contactPhone", b.getContactPhone() != null ? b.getContactPhone() : "");
        event.put("airline", b.getAirline() != null ? b.getAirline() : "");
        event.put("flightNumber", b.getFlightNumber() != null ? b.getFlightNumber() : "");
        event.put("departureCityCode", b.getDepartureCityCode() != null ? b.getDepartureCityCode() : "");
        event.put("arrivalCityCode", b.getArrivalCityCode() != null ? b.getArrivalCityCode() : "");
        event.put("departureCity", b.getDepartureCity() != null ? b.getDepartureCity() : "");
        event.put("arrivalCity", b.getArrivalCity() != null ? b.getArrivalCity() : "");
        event.put("departureDate", b.getDepartureDate().toString());
        event.put("totalAmountPaise", b.getTotalAmountPaise());
        event.put("status", b.getStatus().name());
        event.put("isInternational", b.getIsInternational());
        return event;
    }
}
