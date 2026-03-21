package com.safar.review.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class BookingServiceClientImpl implements BookingServiceClient {

    private final RestTemplate restTemplate;

    public BookingServiceClientImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Value("${services.booking-service.url}")
    private String bookingServiceUrl;

    @Override
    @CircuitBreaker(name = "bookingService", fallbackMethod = "isConfirmedBookingOwnerFallback")
    @Retry(name = "bookingService")
    public boolean isConfirmedBookingOwner(UUID bookingId, UUID guestId) {
        @SuppressWarnings("unchecked")
        Map<String, Object> booking = restTemplate.getForObject(
                bookingServiceUrl + "/api/v1/internal/bookings/" + bookingId, Map.class);
        if (booking == null) return false;
        String status   = (String) booking.get("status");
        String bGuestId = (String) booking.get("guestId");
        boolean validStatus = "CONFIRMED".equals(status)
                || "COMPLETED".equals(status)
                || "CHECKED_IN".equals(status);
        return validStatus && guestId.toString().equals(bGuestId);
    }

    public boolean isConfirmedBookingOwnerFallback(UUID bookingId, UUID guestId, Throwable t) {
        log.warn("Could not reach booking-service for booking {}: {}", bookingId, t.getMessage());
        return false;
    }

    @Override
    @CircuitBreaker(name = "bookingService", fallbackMethod = "getHostIdForBookingFallback")
    @Retry(name = "bookingService")
    public UUID getHostIdForBooking(UUID bookingId) {
        @SuppressWarnings("unchecked")
        Map<String, Object> booking = restTemplate.getForObject(
                bookingServiceUrl + "/api/v1/internal/bookings/" + bookingId, Map.class);
        if (booking == null || booking.get("hostId") == null) return null;
        return UUID.fromString((String) booking.get("hostId"));
    }

    public UUID getHostIdForBookingFallback(UUID bookingId, Throwable t) {
        log.warn("Could not get hostId for booking {}: {}", bookingId, t.getMessage());
        return null;
    }

    @Override
    @CircuitBreaker(name = "bookingService", fallbackMethod = "getListingIdForBookingFallback")
    @Retry(name = "bookingService")
    public UUID getListingIdForBooking(UUID bookingId) {
        @SuppressWarnings("unchecked")
        Map<String, Object> booking = restTemplate.getForObject(
                bookingServiceUrl + "/api/v1/internal/bookings/" + bookingId, Map.class);
        if (booking == null || booking.get("listingId") == null) return null;
        return UUID.fromString((String) booking.get("listingId"));
    }

    public UUID getListingIdForBookingFallback(UUID bookingId, Throwable t) {
        log.warn("Could not get listingId for booking {}: {}", bookingId, t.getMessage());
        return null;
    }

    @Override
    @CircuitBreaker(name = "bookingService", fallbackMethod = "getGuestNameForBookingFallback")
    @Retry(name = "bookingService")
    public String getGuestNameForBooking(UUID bookingId) {
        @SuppressWarnings("unchecked")
        Map<String, Object> booking = restTemplate.getForObject(
                bookingServiceUrl + "/api/v1/internal/bookings/" + bookingId, Map.class);
        if (booking == null) return null;
        String first = (String) booking.get("guestFirstName");
        String last  = (String) booking.get("guestLastName");
        if (first == null && last == null) return null;
        return ((first != null ? first : "") + " " + (last != null ? last : "")).trim();
    }

    public String getGuestNameForBookingFallback(UUID bookingId, Throwable t) {
        log.warn("Could not get guest name for booking {}: {}", bookingId, t.getMessage());
        return null;
    }
}
