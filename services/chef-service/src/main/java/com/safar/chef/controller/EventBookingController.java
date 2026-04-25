package com.safar.chef.controller;

import com.safar.chef.dto.AssignStaffRequest;
import com.safar.chef.dto.AssignVendorRequest;
import com.safar.chef.dto.CreateEventBookingRequest;
import com.safar.chef.dto.MarkVendorPayoutRequest;
import com.safar.chef.dto.ModifyEventBookingRequest;
import com.safar.chef.dto.VendorAssignmentResponse;
import com.safar.chef.entity.EventBooking;
import com.safar.chef.entity.EventBookingStaff;
import com.safar.chef.repository.ChefProfileRepository;
import com.safar.chef.repository.EventBookingStaffRepository;
import com.safar.chef.service.EventBookingService;
import com.safar.chef.service.EventBookingStaffService;
import com.safar.chef.service.EventBookingVendorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chef-events")
@RequiredArgsConstructor
public class EventBookingController {

    private final EventBookingService eventBookingService;
    private final EventBookingStaffService eventBookingStaffService;
    private final EventBookingVendorService eventBookingVendorService;
    private final ChefProfileRepository chefProfileRepo;
    private final EventBookingStaffRepository eventBookingStaffRepo;

    @GetMapping
    public ResponseEntity<Page<EventBooking>> browse(Pageable pageable) {
        return ResponseEntity.ok(eventBookingService.browseEvents(pageable));
    }

    /**
     * Aggregate ratings across all chefs and by staff role. Powers the star
     * badges on /cooks/services flagship cards. Public endpoint — safe data,
     * just averages and counts. Falls back to zero rather than throwing if
     * there's no data yet.
     */
    @GetMapping("/aggregate-ratings")
    public ResponseEntity<Map<String, Map<String, Object>>> aggregateRatings() {
        Map<String, Map<String, Object>> out = new java.util.LinkedHashMap<>();

        // Chef rating from ChefProfile.rating
        Object[] chef = chefProfileRepo.aggregateChefRating();
        out.put("chef", ratingEntry(chef != null && chef.length > 0 ? chef[0] : null,
                                     chef != null && chef.length > 1 ? chef[1] : null));

        // Each staff role from event_booking_staff
        for (Object[] row : eventBookingStaffRepo.aggregateRatingsByRole()) {
            if (row == null || row.length < 3) continue;
            String role = (String) row[0];
            out.put(role, ratingEntry(row[1], row[2]));
        }
        return ResponseEntity.ok(out);
    }

    private static Map<String, Object> ratingEntry(Object avg, Object count) {
        Map<String, Object> m = new java.util.HashMap<>();
        double avgD = avg instanceof Number ? ((Number) avg).doubleValue() : 0.0;
        long countL = count instanceof Number ? ((Number) count).longValue() : 0L;
        m.put("avg",   Math.round(avgD * 10.0) / 10.0);   // 1 decimal
        m.put("count", countL);
        return m;
    }

    private void requireAdmin(Authentication auth) {
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            throw new org.springframework.security.access.AccessDeniedException("Admin access required");
        }
    }

    // ── Admin ─────────────────────────────────────────────────

    @GetMapping("/admin/all")
    public ResponseEntity<Page<EventBooking>> adminListAll(Pageable pageable, Authentication auth) {
        requireAdmin(auth);
        return ResponseEntity.ok(eventBookingService.browseEvents(pageable));
    }

    @PostMapping("/admin/{id}/assign")
    public ResponseEntity<EventBooking> adminAssignChef(@PathVariable UUID id,
                                                         @RequestParam UUID chefId,
                                                         Authentication auth) {
        requireAdmin(auth);
        return ResponseEntity.ok(eventBookingService.adminAssignChef(id, chefId));
    }

    @PostMapping("/admin/{id}/cancel")
    public ResponseEntity<EventBooking> adminCancel(@PathVariable UUID id,
                                                     @RequestParam(required = false) String reason,
                                                     Authentication auth) {
        requireAdmin(auth);
        return ResponseEntity.ok(eventBookingService.adminCancelEvent(id, reason));
    }

    @PostMapping("/admin/{id}/complete")
    public ResponseEntity<EventBooking> adminComplete(@PathVariable UUID id, Authentication auth) {
        requireAdmin(auth);
        return ResponseEntity.ok(eventBookingService.adminCompleteEvent(id));
    }

    @PostMapping
    public ResponseEntity<EventBooking> createEvent(Authentication auth,
                                                     @RequestBody CreateEventBookingRequest req) {
        UUID customerId = auth != null ? UUID.fromString(auth.getName()) : null;
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventBookingService.createEventBooking(customerId, req));
    }

    // ── Staff assignment (chef assigns team members to a booking) ──────
    @GetMapping("/{id}/staff")
    public ResponseEntity<List<com.safar.chef.dto.EventStaffAssignmentResponse>> listStaff(@PathVariable UUID id) {
        return ResponseEntity.ok(eventBookingStaffService.listAssignmentsEnriched(id));
    }

    @PostMapping("/{id}/assign-staff")
    public ResponseEntity<List<EventBookingStaff>> assignStaff(Authentication auth,
                                                                @PathVariable UUID id,
                                                                @RequestBody AssignStaffRequest req) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(eventBookingStaffService.assign(userId, id, req));
    }

    @PostMapping("/{id}/staff/{staffId}/check-in")
    public ResponseEntity<EventBookingStaff> checkInStaff(Authentication auth,
                                                            @PathVariable UUID id,
                                                            @PathVariable UUID staffId,
                                                            @RequestParam(required = false) String otp) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(eventBookingStaffService.checkIn(userId, id, staffId, otp));
    }

    @PostMapping("/{id}/staff/{staffId}/no-show")
    public ResponseEntity<EventBookingStaff> markNoShow(Authentication auth,
                                                         @PathVariable UUID id,
                                                         @PathVariable UUID staffId) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(eventBookingStaffService.markNoShow(userId, id, staffId));
    }

    @PostMapping("/{id}/staff/{staffId}/rate")
    public ResponseEntity<EventBookingStaff> rateStaff(Authentication auth,
                                                        @PathVariable UUID id,
                                                        @PathVariable UUID staffId,
                                                        @RequestParam int stars,
                                                        @RequestParam(required = false) String comment) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(eventBookingStaffService.rate(userId, id, staffId, stars, comment));
    }

    // ── Vendor assignment (admin assigns external vendor to a bespoke booking) ──

    @GetMapping("/{id}/vendor")
    public ResponseEntity<VendorAssignmentResponse> getActiveVendor(@PathVariable UUID id) {
        VendorAssignmentResponse a = eventBookingVendorService.activeForBooking(id);
        if (a == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(a);
    }

    @GetMapping("/{id}/vendors")
    public ResponseEntity<List<VendorAssignmentResponse>> listVendorHistory(Authentication auth,
                                                                             @PathVariable UUID id) {
        requireAdmin(auth);
        return ResponseEntity.ok(eventBookingVendorService.listForBooking(id));
    }

    @PostMapping("/{id}/assign-vendor")
    public ResponseEntity<VendorAssignmentResponse> assignVendor(Authentication auth,
                                                                  @PathVariable UUID id,
                                                                  @RequestBody AssignVendorRequest req) {
        requireAdmin(auth);
        return ResponseEntity.ok(eventBookingVendorService.assign(id, req));
    }

    @PostMapping("/{id}/vendor/{assignmentId}/confirm")
    public ResponseEntity<VendorAssignmentResponse> confirmVendor(Authentication auth,
                                                                   @PathVariable UUID id,
                                                                   @PathVariable UUID assignmentId) {
        requireAdmin(auth);
        return ResponseEntity.ok(eventBookingVendorService.confirm(id, assignmentId));
    }

    @PostMapping("/{id}/vendor/{assignmentId}/delivered")
    public ResponseEntity<VendorAssignmentResponse> markVendorDelivered(Authentication auth,
                                                                         @PathVariable UUID id,
                                                                         @PathVariable UUID assignmentId) {
        requireAdmin(auth);
        return ResponseEntity.ok(eventBookingVendorService.markDelivered(id, assignmentId));
    }

    @PostMapping("/{id}/vendor/{assignmentId}/cancel")
    public ResponseEntity<VendorAssignmentResponse> cancelVendor(Authentication auth,
                                                                  @PathVariable UUID id,
                                                                  @PathVariable UUID assignmentId,
                                                                  @RequestParam(required = false) String reason) {
        requireAdmin(auth);
        return ResponseEntity.ok(eventBookingVendorService.cancel(id, assignmentId, reason));
    }

    @PostMapping("/{id}/vendor/{assignmentId}/mark-paid")
    public ResponseEntity<VendorAssignmentResponse> markVendorPaid(Authentication auth,
                                                                    @PathVariable UUID id,
                                                                    @PathVariable UUID assignmentId,
                                                                    @RequestBody MarkVendorPayoutRequest req) {
        requireAdmin(auth);
        return ResponseEntity.ok(eventBookingVendorService.markPaid(id, assignmentId, req));
    }

    @PostMapping("/{id}/quote")
    public ResponseEntity<EventBooking> quoteEvent(Authentication auth,
                                                    @PathVariable UUID id,
                                                    @RequestParam Long totalAmountPaise) {
        UUID chefId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(eventBookingService.quoteEvent(chefId, id, totalAmountPaise));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<EventBooking> confirmEvent(Authentication auth,
                                                      @PathVariable UUID id) {
        UUID customerId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(eventBookingService.confirmEvent(customerId, id));
    }

    @PostMapping("/{id}/advance-paid")
    public ResponseEntity<EventBooking> markAdvancePaid(Authentication auth,
                                                         @PathVariable UUID id) {
        UUID customerId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(eventBookingService.markAdvancePaid(customerId, id));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<EventBooking> completeEvent(Authentication auth,
                                                       @PathVariable UUID id) {
        UUID chefId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(eventBookingService.completeEvent(chefId, id));
    }

    @PostMapping("/{id}/start-job")
    public ResponseEntity<EventBooking> startJob(Authentication auth,
                                                  @PathVariable UUID id,
                                                  @RequestParam String otp) {
        UUID chefId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(eventBookingService.startJob(chefId, id, otp));
    }

    @PostMapping("/{id}/pay-balance")
    public ResponseEntity<EventBooking> payBalance(Authentication auth,
                                                    @PathVariable UUID id) {
        UUID customerId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(eventBookingService.payBalance(customerId, id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<EventBooking> cancelEvent(Authentication auth,
                                                     @PathVariable UUID id,
                                                     @RequestParam(required = false) String reason) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(eventBookingService.cancelEvent(userId, id, reason));
    }

    @PostMapping("/{id}/rate")
    public ResponseEntity<EventBooking> rateEvent(Authentication auth,
                                                   @PathVariable UUID id,
                                                   @RequestParam int rating,
                                                   @RequestParam(required = false) String comment) {
        UUID customerId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(eventBookingService.rateEvent(customerId, id, rating, comment));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventBooking> modifyEvent(Authentication auth,
                                                     @PathVariable UUID id,
                                                     @RequestBody ModifyEventBookingRequest req) {
        UUID customerId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(eventBookingService.modifyEventBooking(customerId, id, req));
    }

    @GetMapping("/my")
    public ResponseEntity<List<EventBooking>> getMyEvents(Authentication auth) {
        UUID customerId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(eventBookingService.getMyEvents(customerId));
    }

    @GetMapping("/chef")
    public ResponseEntity<List<EventBooking>> getChefEvents(Authentication auth) {
        UUID chefId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(eventBookingService.getChefEvents(chefId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventBooking> get(@PathVariable UUID id) {
        return ResponseEntity.ok(eventBookingService.getEvent(id));
    }
}
