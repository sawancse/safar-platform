package com.safar.user.entity.enums;

public enum ReferralStatus {
    PENDING,    // Code created, not yet used
    SIGNED_UP,  // Referred user signed up
    COMPLETED,  // Qualifying action done (first booking/first hosted stay)
    EXPIRED,    // Code expired (90 days)
    CANCELLED
}
