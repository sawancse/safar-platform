package com.safar.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * A host is payable once the guest has checked out and the dispute window has passed:
 * eligibleAt = booking.checkOut + payouts.dispute-window-days. Fails closed — if the
 * checkout date cannot be determined the line is simply not eligible.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PayoutEligibility {

    private final RestTemplate restTemplate;

    @Value("${services.booking-service.url:http://localhost:8095}")
    private String bookingServiceUrl;
    @Value("${payouts.dispute-window-days:2}")
    private int disputeWindowDays;

    @SuppressWarnings("unchecked")
    public Optional<OffsetDateTime> eligibleAt(UUID bookingId) {
        if (bookingId == null) return Optional.empty();
        try {
            Map<String, Object> b = restTemplate.getForObject(
                    bookingServiceUrl + "/api/v1/internal/bookings/{id}", Map.class, bookingId);
            Object co = b == null ? null : b.get("checkOut");
            if (co == null) return Optional.empty();
            LocalDateTime checkOut = LocalDateTime.parse(co.toString());
            return Optional.of(checkOut.atZone(ZoneId.of("Asia/Kolkata")).toOffsetDateTime().plusDays(disputeWindowDays));
        } catch (Exception e) {
            log.warn("Could not determine checkout for booking {}: {}", bookingId, e.getMessage());
            return Optional.empty();
        }
    }
}
