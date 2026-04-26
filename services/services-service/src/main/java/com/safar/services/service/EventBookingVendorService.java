package com.safar.services.service;

import com.safar.services.dto.AssignVendorRequest;
import com.safar.services.dto.MarkVendorPayoutRequest;
import com.safar.services.dto.VendorAssignmentResponse;
import com.safar.services.entity.EventBooking;
import com.safar.services.entity.EventBookingVendor;
import com.safar.services.entity.PartnerVendor;
import com.safar.services.entity.enums.VendorAssignmentStatus;
import com.safar.services.repository.EventBookingRepository;
import com.safar.services.repository.EventBookingVendorRepository;
import com.safar.services.repository.PartnerVendorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventBookingVendorService {

    private final EventBookingVendorRepository assignmentRepo;
    private final PartnerVendorRepository vendorRepo;
    private final EventBookingRepository bookingRepo;
    private final PartnerVendorService vendorService;
    private final VendorAssignmentNotifier notifier;

    @Transactional(readOnly = true)
    public List<VendorAssignmentResponse> listForBooking(UUID bookingId) {
        return assignmentRepo.findByEventBookingIdOrderByCreatedAtDesc(bookingId).stream()
                .map(a -> VendorAssignmentResponse.from(a, vendorRepo.findById(a.getVendorId()).orElse(null)))
                .toList();
    }

    @Transactional(readOnly = true)
    public VendorAssignmentResponse activeForBooking(UUID bookingId) {
        EventBookingVendor a = assignmentRepo
                .findFirstByEventBookingIdAndStatusNot(bookingId, VendorAssignmentStatus.CANCELLED)
                .orElse(null);
        if (a == null) return null;
        PartnerVendor v = vendorRepo.findById(a.getVendorId()).orElse(null);
        return VendorAssignmentResponse.from(a, v);
    }

    @Transactional
    public VendorAssignmentResponse assign(UUID bookingId, AssignVendorRequest req) {
        EventBooking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));
        PartnerVendor vendor = vendorRepo.findById(req.vendorId())
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found: " + req.vendorId()));
        if (Boolean.FALSE.equals(vendor.getActive())) {
            throw new IllegalStateException("Vendor is inactive");
        }

        // Block double-assignment unless prior was cancelled.
        assignmentRepo.findFirstByEventBookingIdAndStatusNot(bookingId, VendorAssignmentStatus.CANCELLED)
                .ifPresent(existing -> {
                    throw new IllegalStateException("Booking already has an active vendor assignment: " + existing.getId());
                });

        long payout = req.payoutPaise() != null
                ? req.payoutPaise()
                : (booking.getTotalAmountPaise() == null ? 0L : booking.getTotalAmountPaise());

        EventBookingVendor a = EventBookingVendor.builder()
                .eventBookingId(bookingId)
                .vendorId(vendor.getId())
                .status(VendorAssignmentStatus.ASSIGNED)
                .assignedAt(OffsetDateTime.now())
                .payoutPaise(payout)
                .payoutStatus("PENDING")
                .adminNotes(req.adminNotes())
                .build();
        a = assignmentRepo.save(a);

        notifier.publishAssigned(a, vendor, booking);

        return VendorAssignmentResponse.from(a, vendor);
    }

    @Transactional
    public VendorAssignmentResponse confirm(UUID bookingId, UUID assignmentId) {
        EventBookingVendor a = loadActive(bookingId, assignmentId);
        a.setStatus(VendorAssignmentStatus.CONFIRMED);
        a.setConfirmedAt(OffsetDateTime.now());
        a = assignmentRepo.save(a);
        return VendorAssignmentResponse.from(a, vendorRepo.findById(a.getVendorId()).orElse(null));
    }

    @Transactional
    public VendorAssignmentResponse markDelivered(UUID bookingId, UUID assignmentId) {
        EventBookingVendor loaded = loadActive(bookingId, assignmentId);
        loaded.setStatus(VendorAssignmentStatus.DELIVERED);
        loaded.setDeliveredAt(OffsetDateTime.now());
        final EventBookingVendor a = assignmentRepo.save(loaded);

        vendorService.incrementJobsCompleted(a.getVendorId());

        // Mirror customer's existing event rating onto the vendor, if present.
        bookingRepo.findById(bookingId).ifPresent(b -> {
            if (b.getRatingGiven() != null) {
                vendorService.bumpRating(a.getVendorId(), b.getRatingGiven());
            }
        });

        return VendorAssignmentResponse.from(a, vendorRepo.findById(a.getVendorId()).orElse(null));
    }

    @Transactional
    public VendorAssignmentResponse cancel(UUID bookingId, UUID assignmentId, String reason) {
        EventBookingVendor a = loadActive(bookingId, assignmentId);
        a.setStatus(VendorAssignmentStatus.CANCELLED);
        a.setCancelledAt(OffsetDateTime.now());
        a.setCancelReason(reason);
        a = assignmentRepo.save(a);
        return VendorAssignmentResponse.from(a, vendorRepo.findById(a.getVendorId()).orElse(null));
    }

    @Transactional
    public VendorAssignmentResponse markPaid(UUID bookingId, UUID assignmentId, MarkVendorPayoutRequest req) {
        EventBookingVendor a = loadActive(bookingId, assignmentId);
        if (a.getStatus() != VendorAssignmentStatus.DELIVERED) {
            throw new IllegalStateException("Mark delivered before recording payout");
        }
        if (req.payoutPaise() != null) a.setPayoutPaise(req.payoutPaise());
        a.setPayoutRef(req.payoutRef());
        a.setPayoutStatus("PAID");
        a.setPayoutAt(OffsetDateTime.now());
        a = assignmentRepo.save(a);
        return VendorAssignmentResponse.from(a, vendorRepo.findById(a.getVendorId()).orElse(null));
    }

    private EventBookingVendor loadActive(UUID bookingId, UUID assignmentId) {
        EventBookingVendor a = assignmentRepo.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found: " + assignmentId));
        if (!bookingId.equals(a.getEventBookingId())) {
            throw new IllegalArgumentException("Assignment does not belong to booking: " + bookingId);
        }
        return a;
    }
}
