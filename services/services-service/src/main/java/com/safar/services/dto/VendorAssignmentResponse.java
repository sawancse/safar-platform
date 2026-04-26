package com.safar.services.dto;

import com.safar.services.entity.EventBookingVendor;
import com.safar.services.entity.PartnerVendor;
import com.safar.services.entity.enums.VendorAssignmentStatus;
import com.safar.services.entity.enums.VendorServiceType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record VendorAssignmentResponse(
        UUID id,
        UUID eventBookingId,
        UUID vendorId,
        String vendorBusinessName,
        String vendorPhone,
        String vendorEmail,
        VendorServiceType vendorServiceType,
        BigDecimal vendorRatingAvg,
        Integer vendorJobsCompleted,
        VendorAssignmentStatus status,
        OffsetDateTime assignedAt,
        OffsetDateTime confirmedAt,
        OffsetDateTime deliveredAt,
        OffsetDateTime cancelledAt,
        Long payoutPaise,
        String payoutStatus,
        String payoutRef,
        OffsetDateTime payoutAt,
        String adminNotes
) {
    public static VendorAssignmentResponse from(EventBookingVendor a, PartnerVendor v) {
        return new VendorAssignmentResponse(
                a.getId(),
                a.getEventBookingId(),
                a.getVendorId(),
                v != null ? v.getBusinessName() : null,
                v != null ? v.getPhone() : null,
                v != null ? v.getEmail() : null,
                v != null ? v.getServiceType() : null,
                v != null ? v.getRatingAvg() : null,
                v != null ? v.getJobsCompleted() : null,
                a.getStatus(),
                a.getAssignedAt(),
                a.getConfirmedAt(),
                a.getDeliveredAt(),
                a.getCancelledAt(),
                a.getPayoutPaise(),
                a.getPayoutStatus(),
                a.getPayoutRef(),
                a.getPayoutAt(),
                a.getAdminNotes()
        );
    }
}
