package com.safar.notification.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class BookingClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String bookingServiceUrl;

    public BookingClient(RestTemplate restTemplate,
                         ObjectMapper objectMapper,
                         @Value("${services.booking-service.url}") String bookingServiceUrl) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.bookingServiceUrl = bookingServiceUrl;
    }

    @CircuitBreaker(name = "bookingService", fallbackMethod = "getBookingFallback")
    @Retry(name = "bookingService")
    public BookingInfo getBooking(String bookingId) {
        String url = bookingServiceUrl + "/api/v1/internal/bookings/" + bookingId;
        String json = restTemplate.getForObject(url, String.class);
        try {
            JsonNode n = objectMapper.readTree(json);
            return new BookingInfo(
                    n.path("id").asText(),
                    n.path("bookingRef").asText(),
                    n.path("guestId").asText(),
                    n.path("hostId").asText(),
                    n.path("listingId").asText(),
                    n.path("guestEmail").asText(""),
                    n.path("guestFirstName").asText(""),
                    n.path("guestLastName").asText(""),
                    n.path("checkIn").asText(null),
                    n.path("checkOut").asText(null),
                    n.path("nights").asInt(0),
                    n.path("guestsCount").asInt(0),
                    n.path("adultsCount").asInt(0),
                    n.path("childrenCount").asInt(0),
                    n.path("infantsCount").asInt(0),
                    n.path("roomsCount").asInt(0),
                    n.path("listingTitle").asText(""),
                    n.path("listingCity").asText(""),
                    n.path("listingAddress").asText(""),
                    n.path("totalAmountPaise").asLong(0L),
                    n.path("baseAmountPaise").asLong(0L),
                    n.path("gstAmountPaise").asLong(0L),
                    n.path("cleaningFeePaise").asLong(0L),
                    n.path("platformFeePaise").asLong(0L),
                    n.path("insuranceAmountPaise").asLong(0L),
                    n.path("securityDepositPaise").asLong(0L),
                    n.path("inclusionsTotalPaise").asLong(0L),
                    n.path("paymentMode").asText(""),
                    n.path("cancellationReason").asText(""),
                    n.path("pricingUnit").asText("NIGHT")
            );
        } catch (Exception e) {
            log.warn("Failed to parse booking {}: {}", bookingId, e.getMessage());
            return null;
        }
    }

    public BookingInfo getBookingFallback(String bookingId, Throwable t) {
        log.warn("Failed to fetch booking {}: {}", bookingId, t.getMessage());
        return null;
    }

    public record BookingInfo(
            String id, String bookingRef,
            String guestId, String hostId, String listingId,
            String guestEmail, String guestFirstName, String guestLastName,
            String checkIn, String checkOut, int nights,
            int guestsCount, int adultsCount, int childrenCount, int infantsCount,
            int roomsCount,
            String listingTitle, String listingCity, String listingAddress,
            long totalAmountPaise, long baseAmountPaise, long gstAmountPaise,
            long cleaningFeePaise, long platformFeePaise, long insuranceAmountPaise,
            long securityDepositPaise, long inclusionsTotalPaise,
            String paymentMode, String cancellationReason, String pricingUnit
    ) {
        public String guestName() {
            String name = (guestFirstName + " " + guestLastName).trim();
            return name.isEmpty() ? "Guest" : name;
        }
    }
}
