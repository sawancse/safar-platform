package com.safar.flight.controller;

import com.safar.flight.dto.FlightBookingResponse;
import com.safar.flight.entity.FlightBooking;
import com.safar.flight.entity.FlightBookingStatus;
import com.safar.flight.entity.RefundApproval;
import com.safar.flight.repository.FlightBookingRepository;
import com.safar.flight.repository.RefundApprovalRepository;
import com.safar.flight.service.FlightBookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/v1/flights/admin")
@RequiredArgsConstructor
@Slf4j
public class FlightAdminController {

    private final FlightBookingRepository bookingRepository;
    private final RefundApprovalRepository refundApprovalRepository;
    private final FlightBookingService flightService;

    private void requireAdmin(Authentication auth) {
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            throw new AccessDeniedException("Admin access required");
        }
    }

    @GetMapping("/all")
    public ResponseEntity<Page<FlightBooking>> listAll(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) String origin,
            @RequestParam(required = false) String destination,
            Pageable pageable,
            Authentication auth) {
        requireAdmin(auth);

        FlightBookingStatus statusEnum = status != null ? FlightBookingStatus.valueOf(status) : null;
        LocalDate from = fromDate != null ? LocalDate.parse(fromDate) : null;
        LocalDate to = toDate != null ? LocalDate.parse(toDate) : null;

        return ResponseEntity.ok(bookingRepository.adminSearch(statusEnum, from, to, origin, destination, pageable));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats(Authentication auth) {
        requireAdmin(auth);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalBookings", bookingRepository.count());
        stats.put("confirmed", bookingRepository.countByStatus(FlightBookingStatus.CONFIRMED));
        stats.put("cancelled", bookingRepository.countByStatus(FlightBookingStatus.CANCELLED));
        stats.put("pendingPayment", bookingRepository.countByStatus(FlightBookingStatus.PENDING_PAYMENT));
        stats.put("completed", bookingRepository.countByStatus(FlightBookingStatus.COMPLETED));
        stats.put("totalRevenuePaise", bookingRepository.totalRevenuePaise());

        List<Object[]> routes = bookingRepository.topRoutes(PageRequest.of(0, 10));
        List<Map<String, Object>> topRoutes = routes.stream().map(r -> {
            Map<String, Object> route = new LinkedHashMap<>();
            route.put("origin", r[0]);
            route.put("destination", r[1]);
            route.put("count", r[2]);
            return route;
        }).toList();
        stats.put("topRoutes", topRoutes);

        return ResponseEntity.ok(stats);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<FlightBooking> adminCancel(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason,
            Authentication auth) {
        requireAdmin(auth);

        FlightBooking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Booking not found: " + id));

        if (booking.getStatus() == FlightBookingStatus.CANCELLED
                || booking.getStatus() == FlightBookingStatus.REFUNDED) {
            throw new IllegalStateException("Booking already cancelled/refunded");
        }

        booking.setStatus(FlightBookingStatus.CANCELLED);
        booking.setCancellationReason(reason != null ? reason : "Cancelled by admin");
        booking.setCancelledAt(Instant.now());

        if ("PAID".equals(booking.getPaymentStatus())) {
            booking.setPaymentStatus("REFUND_INITIATED");
            booking.setRefundAmountPaise(booking.getTotalAmountPaise());
        }

        bookingRepository.save(booking);
        log.info("Admin cancelled flight booking: {} reason: {}", booking.getBookingRef(), reason);
        return ResponseEntity.ok(booking);
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<FlightBooking> adminRefund(
            @PathVariable UUID id,
            @RequestParam(required = false) Long amountPaise,
            Authentication auth) {
        requireAdmin(auth);

        FlightBooking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Booking not found: " + id));

        long refund = amountPaise != null ? amountPaise : booking.getTotalAmountPaise();
        booking.setRefundAmountPaise(refund);
        booking.setPaymentStatus("REFUND_INITIATED");
        booking.setStatus(FlightBookingStatus.REFUNDED);

        bookingRepository.save(booking);
        log.info("Admin initiated refund for flight booking: {} amount: {} paise", booking.getBookingRef(), refund);
        return ResponseEntity.ok(booking);
    }

    @GetMapping("/revenue")
    public ResponseEntity<Map<String, Object>> revenue(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            Authentication auth) {
        requireAdmin(auth);

        Map<String, Object> revenue = new LinkedHashMap<>();
        revenue.put("totalRevenuePaise", bookingRepository.totalRevenuePaise());
        revenue.put("totalBookings", bookingRepository.count());
        revenue.put("cancelledBookings", bookingRepository.countByStatus(FlightBookingStatus.CANCELLED));
        revenue.put("cancellationRate",
                bookingRepository.count() > 0
                        ? String.format("%.1f%%", bookingRepository.countByStatus(FlightBookingStatus.CANCELLED) * 100.0 / bookingRepository.count())
                        : "0%");
        return ResponseEntity.ok(revenue);
    }

    // ─── Two-step refund approval queue (per Tree-4) ────────────────

    @GetMapping("/refunds/pending")
    public ResponseEntity<Page<RefundApproval>> pendingRefunds(
            @org.springframework.data.web.PageableDefault(size = 20) Pageable pageable,
            Authentication auth) {
        requireAdmin(auth);
        return ResponseEntity.ok(refundApprovalRepository
                .findByStatusOrderByPriorityAscRequestedAtAsc("PENDING", pageable));
    }

    @GetMapping("/refunds/stats")
    public ResponseEntity<Map<String, Object>> refundStats(Authentication auth) {
        requireAdmin(auth);
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("pending", refundApprovalRepository.countByStatus("PENDING"));
        stats.put("approved", refundApprovalRepository.countByStatus("APPROVED"));
        stats.put("rejected", refundApprovalRepository.countByStatus("REJECTED"));
        stats.put("completed", refundApprovalRepository.countByStatus("COMPLETED"));
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/refunds/{id}/approve")
    public ResponseEntity<RefundApproval> approveRefund(
            @PathVariable UUID id,
            @RequestParam(required = false) Long approvedAmountPaise,
            @RequestParam(required = false) String notes,
            Authentication auth) {
        requireAdmin(auth);
        UUID adminId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(flightService.approveRefund(id, adminId, approvedAmountPaise, notes));
    }

    @PostMapping("/refunds/{id}/reject")
    public ResponseEntity<RefundApproval> rejectRefund(
            @PathVariable UUID id,
            @RequestParam(required = false) String notes,
            Authentication auth) {
        requireAdmin(auth);
        UUID adminId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(flightService.rejectRefund(id, adminId, notes));
    }
}
