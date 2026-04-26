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
import com.safar.flight.repository.FlightSearchEventRepository;
import com.safar.flight.repository.RefundApprovalRepository;
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
    private final FlightSearchEventRepository searchEventRepository;
    private final RefundApprovalRepository refundApprovalRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /** Refunds at-or-below this amount auto-confirm; above goes to admin review. Per Tree-4. */
    private static final long AUTO_REFUND_THRESHOLD_PAISE = 10_00_000L;     // ₹10,000
    /** Refunds above this OR international OR group bookings get HIGH priority (4hr SLA). */
    private static final long HIGH_PRIORITY_THRESHOLD_PAISE = 50_00_000L;   // ₹50,000

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
                    .provider(provider.name())
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
        suppressMatchingSearchEvents(userId, booking);
        return toResponse(booking);
    }

    /**
     * When a user books a route they previously searched, mark all matching
     * open search events as suppressed=BOOKED so the AbandonedSearchDetector
     * stops nudging them about a route they've already converted on.
     */
    private void suppressMatchingSearchEvents(UUID userId, FlightBooking booking) {
        try {
            if (userId == null || booking.getDepartureCityCode() == null
                    || booking.getArrivalCityCode() == null
                    || booking.getDepartureDate() == null) {
                return;
            }
            var matches = searchEventRepository
                    .findByUserIdAndOriginAndDestinationAndDepartureDateAndSuppressedFalse(
                            userId,
                            booking.getDepartureCityCode(),
                            booking.getArrivalCityCode(),
                            booking.getDepartureDate());
            for (var event : matches) {
                event.setSuppressed(true);
                event.setSuppressionReason("BOOKED");
            }
            if (!matches.isEmpty()) {
                searchEventRepository.saveAll(matches);
                log.info("Suppressed {} abandoned-search events for booking {}",
                        matches.size(), booking.getBookingRef());
            }
        } catch (Exception e) {
            log.warn("Failed to suppress matching search events (non-fatal): {}", e.getMessage());
        }
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

        // Route cancel to the originating provider (persisted on book).
        // Fallback to primary for legacy rows where provider was never set.
        try {
            FlightProvider bookingProvider = booking.getProvider() != null
                    ? FlightProvider.valueOf(booking.getProvider())
                    : registry.primary().providerType();
            registry.get(bookingProvider).cancel(booking.getDuffelOrderId());
        } catch (Exception e) {
            log.warn("Provider cancel failed for {}; proceeding with local cancel: {}",
                    booking.getBookingRef(), e.getMessage());
        }

        long refundPaise = booking.getTotalAmountPaise() != null ? booking.getTotalAmountPaise() : 0L;
        boolean isPaid = "PAID".equals(booking.getPaymentStatus());

        booking.setCancelledAt(Instant.now());
        booking.setCancellationReason("Cancelled by user");

        if (!isPaid) {
            // No payment to refund — straight to CANCELLED
            booking.setStatus(FlightBookingStatus.CANCELLED);
        } else if (refundPaise <= AUTO_REFUND_THRESHOLD_PAISE) {
            // Auto-confirm small refunds per Tree-4
            booking.setRefundAmountPaise(refundPaise);
            booking.setPaymentStatus("REFUND_INITIATED");
            booking.setStatus(FlightBookingStatus.REFUNDED);
            log.info("Flight booking {} auto-refunded ₹{}", booking.getBookingRef(), refundPaise / 100);
        } else {
            // Above threshold → admin queue
            booking.setStatus(FlightBookingStatus.CANCELLED);
            // Refund amount stays unset until admin approves
            String priority = (refundPaise > HIGH_PRIORITY_THRESHOLD_PAISE
                    || Boolean.TRUE.equals(booking.getIsInternational())) ? "HIGH" : "NORMAL";
            RefundApproval approval = RefundApproval.builder()
                    .flightBookingId(booking.getId())
                    .userId(userId)
                    .requestedAmountPaise(refundPaise)
                    .priority(priority)
                    .reason("User-initiated cancellation")
                    .fareRule("REFUNDABLE") // TODO: derive from provider fare-rules when available
                    .status("PENDING")
                    .build();
            refundApprovalRepository.save(approval);
            log.info("Flight booking {} cancellation queued for admin approval (₹{}, priority={})",
                    booking.getBookingRef(), refundPaise / 100, priority);
        }

        booking = bookingRepository.save(booking);
        log.info("Flight booking cancelled: {}", booking.getBookingRef());
        publishEvent("flight.booking.cancelled", booking);
        return toResponse(booking);
    }

    /** Admin approves a pending refund request — confirms the payout. */
    @Transactional
    public RefundApproval approveRefund(UUID approvalId, UUID adminUserId, Long approvedAmountPaise, String notes) {
        RefundApproval approval = refundApprovalRepository.findById(approvalId)
                .orElseThrow(() -> new NoSuchElementException("Refund approval not found: " + approvalId));
        if (!"PENDING".equals(approval.getStatus())) {
            throw new IllegalStateException("Refund approval is not in PENDING status: " + approval.getStatus());
        }
        long amount = approvedAmountPaise != null && approvedAmountPaise > 0
                ? Math.min(approvedAmountPaise, approval.getRequestedAmountPaise())
                : approval.getRequestedAmountPaise();

        approval.setApprovedAmountPaise(amount);
        approval.setStatus("APPROVED");
        approval.setReviewedAt(Instant.now());
        approval.setReviewedByUserId(adminUserId);
        approval.setReviewNotes(notes);
        approval = refundApprovalRepository.save(approval);

        // Update the linked booking to mark refund initiated
        FlightBooking booking = bookingRepository.findById(approval.getFlightBookingId()).orElse(null);
        if (booking != null) {
            booking.setRefundAmountPaise(amount);
            booking.setPaymentStatus("REFUND_INITIATED");
            booking.setStatus(FlightBookingStatus.REFUNDED);
            bookingRepository.save(booking);
        }
        approval.setStatus("COMPLETED");
        approval.setCompletedAt(Instant.now());
        return refundApprovalRepository.save(approval);
    }

    /** Admin rejects a refund request — booking stays CANCELLED but no refund. */
    @Transactional
    public RefundApproval rejectRefund(UUID approvalId, UUID adminUserId, String notes) {
        RefundApproval approval = refundApprovalRepository.findById(approvalId)
                .orElseThrow(() -> new NoSuchElementException("Refund approval not found: " + approvalId));
        if (!"PENDING".equals(approval.getStatus())) {
            throw new IllegalStateException("Refund approval is not in PENDING status: " + approval.getStatus());
        }
        approval.setStatus("REJECTED");
        approval.setReviewedAt(Instant.now());
        approval.setReviewedByUserId(adminUserId);
        approval.setReviewNotes(notes);
        return refundApprovalRepository.save(approval);
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

    /**
     * Pax count from the persisted passengersJson array. Used by the
     * cross-vertical Trip engine downstream — GROUP rules ("4+ pax wedding
     * bundle") only fire when this is set correctly.
     */
    private int parsePassengerCount(String passengersJson) {
        if (passengersJson == null || passengersJson.isBlank()) return 1;
        try {
            var node = objectMapper.readTree(passengersJson);
            return node.isArray() ? Math.max(1, node.size()) : 1;
        } catch (Exception e) {
            return 1;
        }
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
            event.put("passengerCount", parsePassengerCount(booking.getPassengersJson()));
            if (booking.getRefundAmountPaise() != null) event.put("refundAmountPaise", booking.getRefundAmountPaise());
            if (booking.getPaymentStatus() != null) event.put("paymentStatus", booking.getPaymentStatus());

            kafkaTemplate.send(topic, booking.getId().toString(), objectMapper.writeValueAsString(event));
        } catch (Exception e) {
            log.error("Failed to publish Kafka event: {}", topic, e);
        }
    }
}
