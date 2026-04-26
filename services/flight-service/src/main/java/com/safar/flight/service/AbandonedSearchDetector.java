package com.safar.flight.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safar.flight.entity.FlightSearchEvent;
import com.safar.flight.repository.FlightSearchEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Drives the abandoned-flight-search recovery campaign.
 *
 * Runs every 30 minutes. For each pulse window (1h / 6h / 24h), finds
 * unsuppressed search events of matching age + reminder count, publishes
 * a {@code flight.search.abandoned} Kafka event for each. The
 * notification-service consumes and fans out via Push → WhatsApp → Email.
 *
 * Suppression rules:
 *  - User booked the route: handled by {@link FlightBookingService} on
 *    create — we don't repeat the check here.
 *  - Departure date passed: filtered by repository query.
 *  - reminders_sent >= 3: filtered by repository query.
 *  - User opted out: would be checked here once a preferences API exists.
 *
 * Frequency cap: at most one reminder per event per pulse. If a user
 * searched at 9am and is still un-converted at 3pm, they get 2 reminders
 * total (1h + 6h pulses); if still un-converted next morning they get
 * the third (24h pulse) and then we stop.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AbandonedSearchDetector {

    private static final String TOPIC = "flight.search.abandoned";

    /**
     * Pulse windows: each tuple is (expectedReminders, ageMin, ageMax).
     * For example, the 1h pulse fires when reminders_sent=0 and age is
     * between 1 and 1.5 hrs. The detector runs every 30min so the window
     * is sized to catch at most one detector cycle per event per pulse.
     */
    private static final List<Pulse> PULSES = List.of(
            new Pulse(0, Duration.ofMinutes(60),  Duration.ofMinutes(90)),   // 1-hr pulse
            new Pulse(1, Duration.ofHours(6),     Duration.ofHours(7)),       // 6-hr pulse
            new Pulse(2, Duration.ofHours(24),    Duration.ofHours(25))       // 24-hr pulse
    );

    private final FlightSearchEventRepository searchEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedRateString = "PT30M")
    @Transactional
    public void run() {
        log.info("AbandonedSearchDetector tick — scanning {} pulse windows", PULSES.size());
        LocalDate today = LocalDate.now();
        Instant now = Instant.now();

        int totalFired = 0;
        for (Pulse pulse : PULSES) {
            // Repository expects (maxAge, minAge) where maxAge < minAge in instants
            // (older instant comes first). Translate from "between X and Y hours ago".
            Instant maxAge = now.minus(pulse.ageMax);   // older boundary
            Instant minAge = now.minus(pulse.ageMin);   // newer boundary

            List<FlightSearchEvent> candidates = searchEventRepository.findCandidatesForPulse(
                    pulse.expectedReminders, maxAge, minAge, today);

            for (FlightSearchEvent event : candidates) {
                try {
                    publishAbandonedEvent(event, pulse);
                    event.setRemindersSent(event.getRemindersSent() + 1);
                    event.setLastReminderAt(now);
                    if (event.getRemindersSent() >= 3) {
                        event.setSuppressed(true);
                        event.setSuppressionReason("MAX_REMINDERS");
                    }
                    searchEventRepository.save(event);
                    totalFired++;
                } catch (Exception e) {
                    log.error("Failed to fire abandoned-search reminder for event {}: {}",
                            event.getId(), e.getMessage());
                }
            }
        }
        if (totalFired > 0) {
            log.info("AbandonedSearchDetector fired {} reminders this tick", totalFired);
        }
    }

    private void publishAbandonedEvent(FlightSearchEvent event, Pulse pulse) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventId", event.getId().toString());
        payload.put("pulse", pulseLabel(pulse));
        payload.put("reminderNumber", event.getRemindersSent() + 1);
        payload.put("userId", event.getUserId() != null ? event.getUserId().toString() : "");
        payload.put("deviceId", Optional.ofNullable(event.getDeviceId()).orElse(""));
        payload.put("origin", event.getOrigin());
        payload.put("destination", event.getDestination());
        payload.put("departureDate", event.getDepartureDate().toString());
        payload.put("returnDate", event.getReturnDate() != null ? event.getReturnDate().toString() : "");
        payload.put("paxCount", event.getPaxCount());
        payload.put("cabinClass", Optional.ofNullable(event.getCabinClass()).orElse("ECONOMY"));
        payload.put("cheapestFarePaiseAtSearch",
                event.getCheapestFarePaise() != null ? event.getCheapestFarePaise() : 0);
        payload.put("currency", event.getCurrency());
        payload.put("contactEmail", Optional.ofNullable(event.getContactEmail()).orElse(""));
        payload.put("contactPhone", Optional.ofNullable(event.getContactPhone()).orElse(""));
        payload.put("originCountry", event.getOriginCountry());
        payload.put("destinationCountry", event.getDestinationCountry());
        // Fare-trend signal: DROPPED / STABLE / RISING — defer to consumer or
        // compute here by re-searching. For Day 1 the consumer treats absence
        // as STABLE. To enable: inject FlightSearchService and re-search here,
        // compare to cheapestFarePaiseAtSearch.
        payload.put("fareTrend", "STABLE");
        kafkaTemplate.send(TOPIC, event.getId().toString(), objectMapper.writeValueAsString(payload));
    }

    private static String pulseLabel(Pulse p) {
        long mins = p.ageMin.toMinutes();
        return mins < 120 ? "1H" : mins < 600 ? "6H" : "24H";
    }

    private record Pulse(int expectedReminders, Duration ageMin, Duration ageMax) {}
}
