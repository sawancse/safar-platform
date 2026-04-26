package com.safar.flight.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safar.flight.adapter.FlightProvider;
import com.safar.flight.adapter.FlightProviderAdapter;
import com.safar.flight.adapter.FlightProviderRegistry;
import com.safar.flight.adapter.ProviderBookingResult;
import com.safar.flight.adapter.ProviderOfferId;
import com.safar.flight.dto.CreateFlightBookingRequest;
import com.safar.flight.dto.FlightBookingResponse;
import com.safar.flight.entity.*;
import com.safar.flight.repository.FlightBookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * Owns FlightBooking persistence, payment, and cancel. Delegates all
 * provider HTTP to the {@link FlightProviderAdapter} routed by the
 * {@code PROVIDER:nativeId} prefix on the offer ID.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FlightBookingService {

    private final FlightProviderRegistry registry;
    private final FlightBookingRepository bookingRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public FlightBookingResponse createBooking(UUID userId, CreateFlightBookingRequest request) {
        FlightProvider provider = ProviderOfferId.provider(request.offerId());
        String nativeId = ProviderOfferId.nativeId(request.offerId());
        FlightProviderAdapter adapter = registry.get(provider);

        if (!adapter.canBook()) {
            throw new IllegalStateException(
                    "Provider " + provider + " is redirect-only. Complete booking via the partner deeplinkUrl.");
        }

        ProviderBookingResult result = adapter.book(nativeId, request, userId);
        String bookingRef = generateBookingRef();
        long platformFeePaise = Math.round(result.totalAmountPaise() * 0.02);

        FlightBooking booking;
        try {
            booking = FlightBooking.builder()
                    .userId(userId)
                    .bookingRef(bookingRef)
                    .duffelOrderId(result.externalOrderId())
                    .status(FlightBookingStatus.PENDING_PAYMENT)
                    .tripType(result.tripType() != null ? result.tripType() : TripType.ONE_WAY)
                    .cabinClass(result.cabinClass() != null ? result.cabinClass() : CabinClass.ECONOMY)
                    .departureCity(result.departureCityCode())
                    .departureCityCode(result.departureCityCode())
                    .arrivalCity(result.arrivalCityCode())
                    .arrivalCityCode(result.arrivalCityCode())
                    .departureDate(result.departureDate() != null
                            ? result.departureDate() : java.time.LocalDate.now().plusDays(1))
                    .returnDate(result.returnDate())
                    .airline(result.airline())
                    .flightNumber(result.flightNumber())
                    .isInternational(result.isInternational())
                    .passengersJson(objectMapper.writeValueAsString(request.passengers()))
                    .slicesJson(result.itinerariesJson())
                    .totalAmountPaise(result.totalAmountPaise())
                    .taxPaise(result.taxPaise())
                    .platformFeePaise(platformFeePaise)
                    .currency(result.currency())
                    .contactEmail(request.contactEmail())
                    .contactPhone(request.contactPhone())
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize booking payload", e);
        }

        booking = bookingRepository.save(booking);
        log.info("Flight booking created: {} for user {} (provider={}, externalOrderId={})",
                bookingRef, userId, provider, result.externalOrderId());
        publishEvent("flight.booking.created", booking);
        return toResponse(booking);
    }

    @Transactional
    public FlightBookingResponse confirmPayment(UUID userId, UUID bookingId,
                                                String razorpayOrderId, String razorpayPaymentId) {
        FlightBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NoSuchElementException("Booking not found: " + bookingId));

        if (!booking.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Booking does not belong to this user");
        }
        if (booking.getStatus() != FlightBookingStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("Booking is not in PENDING_PAYMENT status");
        }

        booking.setRazorpayOrderId(razorpayOrderId);
        booking.setRazorpayPaymentId(razorpayPaymentId);
        booking.setPaymentStatus("PAID");
        booking.setStatus(FlightBookingStatus.CONFIRMED);

        booking = bookingRepository.save(booking);
        log.info("Flight booking confirmed: {} payment: {}", booking.getBookingRef(), razorpayPaymentId);
        publishEvent("flight.booking.confirmed", booking);
        return toResponse(booking);
    }

    @Transactional
    public FlightBookingResponse cancelBooking(UUID userId, UUID bookingId) {
        FlightBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NoSuchElementException("Booking not found: " + bookingId));

        if (!booking.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Booking does not belong to this user");
        }
        if (booking.getStatus() == FlightBookingStatus.CANCELLED
                || booking.getStatus() == FlightBookingStatus.REFUNDED) {
            throw new IllegalStateException("Booking is already cancelled/refunded");
        }

        // Best-effort: cancel at primary provider. If we ever persist the
        // originating provider on the booking, route there instead.
        try {
            registry.primary().cancel(booking.getDuffelOrderId());
        } catch (Exception e) {
            log.warn("Provider cancel failed for {}; proceeding with local cancel: {}",
                    booking.getBookingRef(), e.getMessage());
        }

        booking.setStatus(FlightBookingStatus.CANCELLED);
        booking.setCancelledAt(Instant.now());
        booking.setCancellationReason("Cancelled by user");
        booking.setRefundAmountPaise(booking.getTotalAmountPaise());

        if ("PAID".equals(booking.getPaymentStatus())) {
            booking.setPaymentStatus("REFUND_INITIATED");
            booking.setStatus(FlightBookingStatus.REFUNDED);
        }

        booking = bookingRepository.save(booking);
        log.info("Flight booking cancelled: {}", booking.getBookingRef());
        publishEvent("flight.booking.cancelled", booking);
        return toResponse(booking);
    }

    public Page<FlightBookingResponse> getMyBookings(UUID userId, Pageable pageable) {
        return bookingRepository.findByUserId(userId, pageable).map(this::toResponse);
    }

    public FlightBookingResponse getBooking(UUID bookingId) {
        FlightBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NoSuchElementException("Booking not found: " + bookingId));
        return toResponse(booking);
    }

    // ─── Helpers ────────────────────────────────────────────────────

    private String generateBookingRef() {
        return "SF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private FlightBookingResponse toResponse(FlightBooking b) {
        return new FlightBookingResponse(
                b.getId(), b.getUserId(), b.getBookingRef(), b.getDuffelOrderId(),
                b.getStatus(), b.getTripType(), b.getCabinClass(),
                b.getDepartureCity(), b.getDepartureCityCode(),
                b.getArrivalCity(), b.getArrivalCityCode(),
                b.getDepartureDate(), b.getReturnDate(),
                b.getAirline(), b.getFlightNumber(), b.getIsInternational(),
                b.getPassengersJson(), b.getSlicesJson(),
                b.getTotalAmountPaise(), b.getTaxPaise(), b.getPlatformFeePaise(),
                b.getCurrency(), b.getRazorpayOrderId(), b.getRazorpayPaymentId(),
                b.getPaymentStatus(), b.getContactEmail(), b.getContactPhone(),
                b.getCancellationReason(), b.getCancelledAt(), b.getRefundAmountPaise(),
                b.getCreatedAt(), b.getUpdatedAt()
        );
    }

    private void publishEvent(String topic, FlightBooking booking) {
        try {
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("bookingId", booking.getId().toString());
            event.put("bookingRef", booking.getBookingRef());
            event.put("userId", booking.getUserId().toString());
            event.put("status", booking.getStatus().name());
            event.put("totalAmountPaise", booking.getTotalAmountPaise());
            event.put("taxPaise", booking.getTaxPaise());
            event.put("platformFeePaise", booking.getPlatformFeePaise());
            event.put("currency", Optional.ofNullable(booking.getCurrency()).orElse("INR"));
            event.put("airline", Optional.ofNullable(booking.getAirline()).orElse(""));
            event.put("flightNumber", Optional.ofNullable(booking.getFlightNumber()).orElse(""));
            event.put("departureCity", Optional.ofNullable(booking.getDepartureCity()).orElse(""));
            event.put("departureCityCode", Optional.ofNullable(booking.getDepartureCityCode()).orElse(""));
            event.put("arrivalCity", Optional.ofNullable(booking.getArrivalCity()).orElse(""));
            event.put("arrivalCityCode", Optional.ofNullable(booking.getArrivalCityCode()).orElse(""));
            event.put("departureDate", booking.getDepartureDate().toString());
            event.put("returnDate", booking.getReturnDate() != null ? booking.getReturnDate().toString() : "");
            event.put("tripType", booking.getTripType().name());
            event.put("isInternational", booking.getIsInternational());
            event.put("contactEmail", Optional.ofNullable(booking.getContactEmail()).orElse(""));
            event.put("contactPhone", Optional.ofNullable(booking.getContactPhone()).orElse(""));
            if (booking.getRefundAmountPaise() != null) event.put("refundAmountPaise", booking.getRefundAmountPaise());
            if (booking.getPaymentStatus() != null) event.put("paymentStatus", booking.getPaymentStatus());

            kafkaTemplate.send(topic, booking.getId().toString(), objectMapper.writeValueAsString(event));
        } catch (Exception e) {
            log.error("Failed to publish Kafka event: {}", topic, e);
        }
    }
}
