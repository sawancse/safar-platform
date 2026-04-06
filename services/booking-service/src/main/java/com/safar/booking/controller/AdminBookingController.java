package com.safar.booking.controller;

import com.safar.booking.dto.BookingResponse;
import com.safar.booking.entity.Booking;
import com.safar.booking.entity.enums.BookingStatus;
import com.safar.booking.repository.BookingRepository;
import com.safar.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminBookingController {

    private final BookingService bookingService;
    private final BookingRepository bookingRepository;

    // ── All bookings (paginated, filterable) ─────────────────────────────────

    @GetMapping("/bookings")
    public ResponseEntity<Page<BookingResponse>> getAllBookings(
            Authentication auth,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID hostId,
            @RequestParam(required = false) UUID guestId,
            @RequestParam(required = false) UUID listingId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        requireAdmin(auth);
        return ResponseEntity.ok(bookingService.searchAllBookings(
                status, hostId, guestId, listingId, dateFrom, dateTo, search, sortBy, sortDir, page, size));
    }

    // ── Single booking detail ────────────────────────────────────────────────

    @GetMapping("/bookings/{id}")
    public ResponseEntity<BookingResponse> getBooking(Authentication auth, @PathVariable UUID id) {
        requireAdmin(auth);
        return ResponseEntity.ok(bookingService.getBooking(id));
    }

    // ── Bookings by host ─────────────────────────────────────────────────────

    @GetMapping("/bookings/by-host/{hostId}")
    public ResponseEntity<List<BookingResponse>> getBookingsByHost(Authentication auth, @PathVariable UUID hostId) {
        requireAdmin(auth);
        return ResponseEntity.ok(bookingService.getBookingsByHostForAdmin(hostId));
    }

    // ── Bookings by guest ────────────────────────────────────────────────────

    @GetMapping("/bookings/by-guest/{guestId}")
    public ResponseEntity<List<BookingResponse>> getBookingsByGuest(Authentication auth, @PathVariable UUID guestId) {
        requireAdmin(auth);
        return ResponseEntity.ok(bookingService.getBookingsByGuestForAdmin(guestId));
    }

    // ── Admin cancel ─────────────────────────────────────────────────────────

    @PostMapping("/bookings/{id}/cancel")
    public ResponseEntity<BookingResponse> adminCancel(
            Authentication auth,
            @PathVariable UUID id,
            @RequestParam(required = false) String reason) {
        requireAdmin(auth);
        return ResponseEntity.ok(bookingService.adminCancelBooking(id, reason));
    }

    // ── Admin deposit refund ──────────────────────────────────────────────────

    @PostMapping("/bookings/{id}/deposit-refund")
    public ResponseEntity<BookingResponse> adminDepositRefund(
            Authentication auth,
            @PathVariable UUID id,
            @RequestParam(defaultValue = "FULL") String refundType,
            @RequestParam(required = false) Long deductionPaise,
            @RequestParam(required = false) String deductionReason) {
        requireAdmin(auth);
        return ResponseEntity.ok(bookingService.adminRefundDeposit(id, refundType, deductionPaise, deductionReason));
    }

    // ── Bookings with pending deposits ──────────────────────────────────────

    @GetMapping("/bookings/pending-deposits")
    public ResponseEntity<Page<BookingResponse>> getPendingDeposits(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        requireAdmin(auth);
        return ResponseEntity.ok(bookingService.getBookingsWithPendingDeposits(page, size));
    }

    // ── Stats for dashboard ──────────────────────────────────────────────────

    @GetMapping("/bookings/stats")
    public ResponseEntity<Map<String, Object>> getStats(
            Authentication auth,
            @RequestParam(defaultValue = "30") int days) {
        requireAdmin(auth);

        List<Booking> all = bookingRepository.findAll();

        // Counts by status
        Map<String, Long> byStatus = all.stream()
                .collect(Collectors.groupingBy(b -> b.getStatus().name(), Collectors.counting()));

        // Daily trend for last N days
        java.time.OffsetDateTime cutoff = LocalDate.now().minusDays(days).atStartOfDay()
                .atOffset(java.time.ZoneOffset.UTC);
        List<Map<String, Object>> trend = all.stream()
                .filter(b -> b.getCreatedAt() != null && b.getCreatedAt().isAfter(cutoff))
                .collect(Collectors.groupingBy(b -> b.getCreatedAt().toLocalDate().toString()))
                .entrySet().stream()
                .map(e -> {
                    Map<String, Object> day = new LinkedHashMap<>();
                    day.put("date", e.getKey());
                    day.put("count", e.getValue().size());
                    day.put("revenuePaise", e.getValue().stream()
                            .mapToLong(b -> b.getTotalAmountPaise() != null ? b.getTotalAmountPaise() : 0).sum());
                    return day;
                })
                .sorted(Comparator.comparing(m -> (String) m.get("date")))
                .toList();

        // Total revenue
        long totalRevenue = all.stream()
                .mapToLong(b -> b.getTotalAmountPaise() != null ? b.getTotalAmountPaise() : 0).sum();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", all.size());
        result.put("byStatus", byStatus);
        result.put("totalRevenuePaise", totalRevenue);
        result.put("trend", trend);

        return ResponseEntity.ok(result);
    }

    // ── Unique guests ────────────────────────────────────────────────────────

    @GetMapping("/guests")
    public ResponseEntity<List<Map<String, Object>>> getGuests(Authentication auth) {
        requireAdmin(auth);

        List<Booking> all = bookingRepository.findAll();
        Map<UUID, List<Booking>> byGuest = all.stream()
                .collect(Collectors.groupingBy(Booking::getGuestId));

        List<Map<String, Object>> guests = byGuest.entrySet().stream().map(e -> {
            UUID guestId = e.getKey();
            List<Booking> bookings = e.getValue();
            Booking latest = bookings.stream()
                    .max(Comparator.comparing(b -> b.getCreatedAt() != null ? b.getCreatedAt() : java.time.OffsetDateTime.MIN))
                    .orElse(bookings.get(0));

            Map<String, Object> guest = new LinkedHashMap<>();
            guest.put("guestId", guestId);
            guest.put("name", (latest.getGuestFirstName() != null ? latest.getGuestFirstName() : "") + " "
                    + (latest.getGuestLastName() != null ? latest.getGuestLastName() : ""));
            guest.put("email", latest.getGuestEmail());
            guest.put("phone", latest.getGuestPhone());
            guest.put("totalBookings", bookings.size());
            guest.put("totalSpendPaise", bookings.stream()
                    .mapToLong(b -> b.getTotalAmountPaise() != null ? b.getTotalAmountPaise() : 0).sum());
            guest.put("lastBookingDate", latest.getCreatedAt() != null ? latest.getCreatedAt().toString() : null);
            return guest;
        }).sorted((a, b) -> Integer.compare((int) b.get("totalBookings"), (int) a.get("totalBookings")))
                .toList();

        return ResponseEntity.ok(guests);
    }

    private void requireAdmin(Authentication auth) {
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            throw new AccessDeniedException("Admin access required");
        }
    }
}
