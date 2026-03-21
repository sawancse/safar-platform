package com.safar.listing.entity.enums;

public enum PaymentMode {
    PREPAID,          // Pay full amount at booking (default)
    PAY_AT_PROPERTY,  // Pay at check-in (cash/UPI)
    PARTIAL_PREPAID   // First night now, rest at property
}
