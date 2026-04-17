package com.safar.listing.entity.enums;

public enum SalePropertyStatus {
    PENDING,      // Host submitted, awaiting admin review
    VERIFIED,     // Admin approved, visible in search
    ACTIVE,       // Legacy alias for VERIFIED (backward compat with existing DB rows)
    DRAFT,        // Unpublished by host
    PAUSED,       // Temporarily hidden by host
    REJECTED,     // Admin rejected (with reason)
    ARCHIVED,     // Host archived (reversible)
    SUSPENDED,    // Admin suspended (policy violation)
    SOLD,         // Marked as sold
    EXPIRED       // Auto-expired after 90 days
}
