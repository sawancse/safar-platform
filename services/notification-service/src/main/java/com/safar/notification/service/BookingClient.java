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
                         @Value("${services.booking.url}") String bookingServiceUrl) {
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
            JsonNode node = objectMapper.readTree(json);
            return new BookingInfo(
                    node.path("id").asText(),
                    node.path("bookingRef").asText(),
                    node.path("guestId").asText(),
                    node.path("hostId").asText(),
                    node.path("listingId").asText(),
                    node.path("guestEmail").asText(""),
                    node.path("guestFirstName").asText(""),
                    node.path("guestLastName").asText("")
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
            String guestEmail, String guestFirstName, String guestLastName
    ) {
        public String guestName() {
            String name = (guestFirstName + " " + guestLastName).trim();
            return name.isEmpty() ? "Guest" : name;
        }
    }
}
