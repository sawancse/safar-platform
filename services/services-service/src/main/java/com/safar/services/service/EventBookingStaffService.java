package com.safar.services.service;

import com.safar.services.dto.AssignStaffRequest;
import com.safar.services.dto.EventStaffAssignmentResponse;
import com.safar.services.entity.ChefProfile;
import com.safar.services.entity.EventBooking;
import com.safar.services.entity.EventBookingStaff;
import com.safar.services.entity.StaffMember;
import com.safar.services.repository.ChefProfileRepository;
import com.safar.services.repository.EventBookingRepository;
import com.safar.services.repository.EventBookingStaffRepository;
import com.safar.services.repository.StaffMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventBookingStaffService {

    private final EventBookingRepository eventRepo;
    private final EventBookingStaffRepository assignRepo;
    private final StaffMemberRepository staffRepo;
    private final ChefProfileRepository chefProfileRepo;

    private static final SecureRandom RANDOM = new SecureRandom();

    private UUID resolveChefId(UUID userId) {
        ChefProfile chef = chefProfileRepo.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("No chef profile for user " + userId));
        return chef.getId();
    }

    @Transactional(readOnly = true)
    public List<EventBookingStaff> listAssignments(UUID bookingId) {
        return assignRepo.findByBookingId(bookingId);
    }

    /**
     * Same as listAssignments but joined with StaffMember so UIs get name/
     * photo/phone in one call.
     */
    @Transactional(readOnly = true)
    public List<EventStaffAssignmentResponse> listAssignmentsEnriched(UUID bookingId) {
        List<EventBookingStaff> rows = assignRepo.findByBookingId(bookingId);
        if (rows.isEmpty()) return List.of();
        List<UUID> staffIds = rows.stream().map(EventBookingStaff::getStaffId).distinct().toList();
        java.util.Map<UUID, StaffMember> byId = staffRepo.findAllById(staffIds).stream()
                .collect(java.util.stream.Collectors.toMap(StaffMember::getId, s -> s));
        return rows.stream()
                .map(a -> EventStaffAssignmentResponse.from(a, byId.get(a.getStaffId())))
                .toList();
    }

    /**
     * Chef assigns people from their roster to a booking. Replaces any prior
     * assignments for this booking. Validates every staff member is owned by
     * the assigning chef and the booking belongs to the same chef.
     */
    @Transactional
    public List<EventBookingStaff> assign(UUID userId, UUID bookingId, AssignStaffRequest req) {
        UUID chefId = resolveChefId(userId);

        EventBooking booking = eventRepo.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));
        if (booking.getChefId() == null || !chefId.equals(booking.getChefId())) {
            throw new AccessDeniedException("Booking not assigned to you");
        }

        assignRepo.deleteByBookingId(bookingId);
        // Flush the DELETE to the DB before we INSERT — otherwise Hibernate
        // may reorder the statements and the (booking_id, staff_id) unique
        // constraint will trip when the same staff member is being
        // re-assigned to the same booking.
        assignRepo.flush();

        if (req == null || req.assignments() == null || req.assignments().isEmpty()) {
            return List.of();
        }

        OffsetDateTime now = OffsetDateTime.now();
        List<EventBookingStaff> toSave = req.assignments().stream().map(a -> {
            StaffMember member = staffRepo.findById(a.staffId())
                    .orElseThrow(() -> new IllegalArgumentException("Staff not found: " + a.staffId()));
            // Allow chef's own staff OR platform-pool staff (chefId == null).
            if (member.getChefId() != null && !chefId.equals(member.getChefId())) {
                throw new AccessDeniedException("Staff " + a.staffId() + " doesn't belong to you or the platform pool");
            }
            if (!Boolean.TRUE.equals(member.getActive())) {
                throw new IllegalStateException("Staff " + a.staffId() + " is inactive");
            }
            String role = a.role() != null ? a.role() : member.getRole();
            Long rate = a.ratePaise() != null ? a.ratePaise() : member.getHourlyRatePaise();
            if (rate == null) rate = 0L;
            return EventBookingStaff.builder()
                    .bookingId(bookingId)
                    .staffId(a.staffId())
                    .role(role)
                    .ratePaise(rate)
                    .assignedAt(now)
                    .checkInOtp(generateOtp())
                    .noShow(false)
                    .build();
        }).toList();

        return assignRepo.saveAll(toSave);
    }

    private String generateOtp() {
        StringBuilder sb = new StringBuilder(4);
        for (int i = 0; i < 4; i++) sb.append(RANDOM.nextInt(10));
        return sb.toString();
    }

    /**
     * Check a staff member in for a booking. Accepts either the chef who owns
     * the booking OR the customer who booked it. Customer must supply the OTP
     * displayed in their booking-detail page; chef can bypass OTP (they own
     * the assignment).
     */
    @Transactional
    public EventBookingStaff checkIn(UUID userId, UUID bookingId, UUID staffId, String otp) {
        EventBooking booking = eventRepo.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));
        boolean isChef = booking.getChefId() != null && userId.equals(resolveChefUserId(booking.getChefId()));
        boolean isCustomer = userId.equals(booking.getCustomerId());
        if (!isChef && !isCustomer) {
            throw new AccessDeniedException("Not your booking");
        }

        EventBookingStaff row = assignRepo.findByBookingId(bookingId).stream()
                .filter(a -> a.getStaffId().equals(staffId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Staff not assigned to this booking"));

        // Customers must match the stored OTP. Chef can check-in without OTP.
        if (!isChef) {
            String submitted = otp == null ? null : otp.trim();
            if (submitted == null || submitted.isEmpty() || !submitted.equals(row.getCheckInOtp())) {
                throw new IllegalArgumentException("Invalid OTP");
            }
        }
        row.setCheckInAt(OffsetDateTime.now());
        row.setNoShow(false);
        return assignRepo.save(row);
    }

    @Transactional
    public EventBookingStaff markNoShow(UUID userId, UUID bookingId, UUID staffId) {
        EventBooking booking = eventRepo.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));
        if (booking.getChefId() == null || !userId.equals(resolveChefUserId(booking.getChefId()))) {
            throw new AccessDeniedException("Only the assigned chef can mark no-show");
        }
        EventBookingStaff row = assignRepo.findByBookingId(bookingId).stream()
                .filter(a -> a.getStaffId().equals(staffId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Staff not assigned to this booking"));
        row.setNoShow(true);
        row.setCheckInAt(null);
        return assignRepo.save(row);
    }

    @Transactional
    public EventBookingStaff rate(UUID userId, UUID bookingId, UUID staffId, int stars, String comment) {
        if (stars < 1 || stars > 5) throw new IllegalArgumentException("Rating must be 1-5");
        EventBooking booking = eventRepo.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));
        if (!userId.equals(booking.getCustomerId())) {
            throw new AccessDeniedException("Only the customer can rate staff");
        }
        EventBookingStaff row = assignRepo.findByBookingId(bookingId).stream()
                .filter(a -> a.getStaffId().equals(staffId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Staff not assigned to this booking"));
        row.setRating((short) stars);
        row.setRatingComment(comment);
        row.setRatedAt(OffsetDateTime.now());
        return assignRepo.save(row);
    }

    private UUID resolveChefUserId(UUID chefProfileId) {
        return chefProfileRepo.findById(chefProfileId).map(ChefProfile::getUserId).orElse(null);
    }
}
