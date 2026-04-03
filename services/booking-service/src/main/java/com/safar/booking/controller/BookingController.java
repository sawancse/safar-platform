package com.safar.booking.controller;

import com.safar.booking.dto.*;
import com.safar.booking.entity.VideoReview;
import com.safar.booking.service.BookingService;
import com.safar.booking.service.BookingService.CalendarEntry;
import com.safar.booking.service.OccupancyReportService;
import com.safar.booking.service.VideoReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final VideoReviewService videoReviewService;
    private final OccupancyReportService occupancyReportService;

    @PostMapping
    public ResponseEntity<BookingResponse> create(Authentication auth,
                                                   @Valid @RequestBody CreateBookingRequest req) {
        UUID guestId = UUID.fromString(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingService.createBooking(guestId, req));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(bookingService.getBooking(id));
    }

    @GetMapping("/me")
    public ResponseEntity<List<BookingResponse>> getMyBookings(Authentication auth) {
        UUID guestId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(bookingService.getMyBookings(guestId));
    }

    @GetMapping("/host")
    public ResponseEntity<List<BookingResponse>> getHostBookings(Authentication auth) {
        UUID hostId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(bookingService.getHostBookings(hostId));
    }

    @GetMapping("/host/search")
    public ResponseEntity<Page<BookingResponse>> searchHostBookings(
            Authentication auth,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID listingId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "checkIn") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID hostId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(bookingService.searchHostBookings(
                hostId, status, listingId, dateFrom, dateTo, search, sortBy, sortDir, page, size));
    }

    @GetMapping("/host/calendar")
    public ResponseEntity<List<CalendarEntry>> getHostCalendar(
            Authentication auth,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        UUID hostId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(bookingService.getHostCalendar(hostId, from, to));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<BookingResponse> confirm(@PathVariable UUID id) {
        return ResponseEntity.ok(bookingService.confirmBooking(id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<BookingResponse> cancel(Authentication auth,
                                                   @PathVariable UUID id,
                                                   @RequestParam(required = false) String reason) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(bookingService.cancelBooking(id, userId, reason));
    }

    @PostMapping("/{id}/check-in")
    public ResponseEntity<BookingResponse> checkIn(Authentication auth, @PathVariable UUID id) {
        UUID hostId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(bookingService.checkInBooking(id, hostId));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<BookingResponse> complete(Authentication auth, @PathVariable UUID id) {
        UUID hostId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(bookingService.completeBooking(id, hostId));
    }

    @PostMapping("/{id}/no-show")
    public ResponseEntity<BookingResponse> noShow(Authentication auth, @PathVariable UUID id) {
        UUID hostId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(bookingService.markNoShow(id, hostId));
    }

    /** Refund security deposit (full or partial). Host initiates after checkout. */
    @PostMapping("/{id}/deposit-refund")
    public ResponseEntity<BookingResponse> depositRefund(
            Authentication auth, @PathVariable UUID id,
            @RequestParam(defaultValue = "FULL") String refundType,
            @RequestParam(required = false) Long deductionPaise,
            @RequestParam(required = false) String deductionReason) {
        UUID hostId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(bookingService.refundDeposit(id, hostId, refundType, deductionPaise, deductionReason));
    }

    // ── S19: Video Reviews ────────────────────────────────────────────────────

    @PostMapping("/{id}/video-review")
    public ResponseEntity<VideoReview> submitVideoReview(Authentication auth,
                                                          @PathVariable UUID id,
                                                          @Valid @RequestBody SubmitVideoReviewRequest req) {
        UUID guestId = UUID.fromString(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(videoReviewService.submitVideoReview(id, guestId, req));
    }

    // ── S20: Apply wallet credits ─────────────────────────────────────────────

    @PostMapping("/{id}/apply-wallet")
    public ResponseEntity<BookingResponse> applyWallet(Authentication auth,
                                                        @PathVariable UUID id,
                                                        @Valid @RequestBody ApplyWalletRequest req) {
        UUID guestId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(bookingService.applyWalletCredits(id, guestId, req.creditsToApplyPaise()));
    }

    // ── Host Occupancy Report ────────────────────────────────────────────────

    @GetMapping("/host/occupancy")
    public ResponseEntity<OccupancyReportDto> getOccupancyReport(
            Authentication auth,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        UUID hostId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(occupancyReportService.getOccupancyReport(hostId, from, to));
    }

    // ── Host Analytics ──────────────────────────────────────────────────────

    @GetMapping("/host/analytics")
    public ResponseEntity<Map<String, Object>> getHostAnalytics(
            Authentication auth,
            @RequestParam(defaultValue = "30") int days) {
        UUID hostId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(bookingService.getHostAnalytics(hostId, days));
    }

    // ── Guest Management ─────────────────────────────────────────

    @GetMapping("/{id}/guests")
    public ResponseEntity<List<BookingGuestResponse>> getGuests(
            Authentication auth, @PathVariable UUID id) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(bookingService.getBookingGuests(id, userId));
    }

    @PostMapping("/{id}/guests")
    public ResponseEntity<BookingGuestResponse> addGuest(
            Authentication auth, @PathVariable UUID id,
            @RequestBody CreateBookingRequest.GuestInfo guest) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingService.addBookingGuest(id, userId, guest));
    }

    @PutMapping("/{id}/guests/{guestId}")
    public ResponseEntity<BookingGuestResponse> updateGuest(
            Authentication auth, @PathVariable UUID id, @PathVariable UUID guestId,
            @RequestBody CreateBookingRequest.GuestInfo guest) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(bookingService.updateBookingGuest(id, guestId, userId, guest));
    }

    @DeleteMapping("/{id}/guests/{guestId}")
    public ResponseEntity<Void> removeGuest(
            Authentication auth, @PathVariable UUID id, @PathVariable UUID guestId) {
        UUID userId = UUID.fromString(auth.getName());
        bookingService.removeBookingGuest(id, guestId, userId);
        return ResponseEntity.noContent().build();
    }
}
