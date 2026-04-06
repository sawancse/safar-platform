package com.safar.chef.controller;

import com.safar.chef.dto.CreateChefBookingRequest;
import com.safar.chef.dto.ModifyChefBookingRequest;
import com.safar.chef.entity.ChefBooking;
import com.safar.chef.service.ChefBookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chef-bookings")
@RequiredArgsConstructor
public class ChefBookingController {

    private final ChefBookingService chefBookingService;

    // ── Admin ─────────────────────────────────────────────────

    @GetMapping("/admin/all")
    public ResponseEntity<Page<ChefBooking>> adminListAll(Pageable pageable) {
        return ResponseEntity.ok(chefBookingService.adminListAll(pageable));
    }

    @PostMapping("/admin/{id}/assign")
    public ResponseEntity<ChefBooking> adminAssignChef(@PathVariable UUID id,
                                                        @RequestParam UUID chefId) {
        return ResponseEntity.ok(chefBookingService.adminAssignChef(id, chefId));
    }

    @PostMapping
    public ResponseEntity<ChefBooking> createBooking(Authentication auth,
                                                      @RequestBody CreateChefBookingRequest req) {
        UUID customerId = UUID.fromString(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chefBookingService.createBooking(customerId, req));
    }

    @PostMapping("/{id}/confirm-payment")
    public ResponseEntity<ChefBooking> confirmPayment(Authentication auth,
                                                       @PathVariable UUID id,
                                                       @RequestParam String razorpayOrderId,
                                                       @RequestParam String razorpayPaymentId) {
        UUID customerId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(chefBookingService.confirmPayment(customerId, id, razorpayOrderId, razorpayPaymentId));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<ChefBooking> confirmBooking(Authentication auth,
                                                       @PathVariable UUID id) {
        UUID chefId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(chefBookingService.confirmBooking(chefId, id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ChefBooking> cancelBooking(Authentication auth,
                                                      @PathVariable UUID id,
                                                      @RequestParam(required = false) String reason) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(chefBookingService.cancelBooking(userId, id, reason));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<ChefBooking> completeBooking(Authentication auth,
                                                        @PathVariable UUID id) {
        UUID chefId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(chefBookingService.completeBooking(chefId, id));
    }

    @PostMapping("/{id}/rate")
    public ResponseEntity<ChefBooking> rateBooking(Authentication auth,
                                                    @PathVariable UUID id,
                                                    @RequestParam int rating,
                                                    @RequestParam(required = false) String comment) {
        UUID customerId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(chefBookingService.rateBooking(customerId, id, rating, comment));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ChefBooking> modifyBooking(Authentication auth,
                                                      @PathVariable UUID id,
                                                      @RequestBody ModifyChefBookingRequest req) {
        UUID customerId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(chefBookingService.modifyBooking(customerId, id, req));
    }

    @GetMapping("/my")
    public ResponseEntity<List<ChefBooking>> getMyBookings(Authentication auth) {
        UUID customerId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(chefBookingService.getMyBookings(customerId));
    }

    @GetMapping("/chef")
    public ResponseEntity<List<ChefBooking>> getChefBookings(Authentication auth) {
        UUID chefId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(chefBookingService.getChefBookings(chefId));
    }

    @PostMapping("/{id}/rebook")
    public ResponseEntity<ChefBooking> rebook(Authentication auth,
                                               @PathVariable UUID id,
                                               @RequestParam LocalDate newDate,
                                               @RequestParam(required = false) String newTime) {
        UUID customerId = UUID.fromString(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chefBookingService.rebook(customerId, id, newDate, newTime));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChefBooking> getBooking(@PathVariable UUID id) {
        return ResponseEntity.ok(chefBookingService.getBooking(id));
    }
}
