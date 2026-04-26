package com.safar.services.dto;

import com.safar.services.entity.EventBookingStaff;
import com.safar.services.entity.StaffMember;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Flattened view of an assignment + the staff member's profile — so the
 * customer and chef can see who's coming without a second fetch.
 */
public record EventStaffAssignmentResponse(
        UUID id,
        UUID bookingId,
        UUID staffId,
        String role,
        Long ratePaise,
        OffsetDateTime assignedAt,
        OffsetDateTime checkInAt,
        String checkInOtp,
        Short rating,
        OffsetDateTime ratedAt,
        String ratingComment,
        Boolean noShow,

        // Staff member fields (nullable if the row was deleted)
        String name,
        String phone,
        String photoUrl,
        String kycStatus,
        Integer yearsExperience,
        String languages,
        Boolean poolMember
) {
    public static EventStaffAssignmentResponse from(EventBookingStaff a, StaffMember m) {
        return new EventStaffAssignmentResponse(
                a.getId(), a.getBookingId(), a.getStaffId(), a.getRole(), a.getRatePaise(),
                a.getAssignedAt(), a.getCheckInAt(), a.getCheckInOtp(),
                a.getRating(), a.getRatedAt(), a.getRatingComment(),
                a.getNoShow(),
                m != null ? m.getName() : null,
                m != null ? m.getPhone() : null,
                m != null ? m.getPhotoUrl() : null,
                m != null ? m.getKycStatus() : null,
                m != null ? m.getYearsExperience() : null,
                m != null ? m.getLanguages() : null,
                m != null ? m.getChefId() == null : null
        );
    }
}
