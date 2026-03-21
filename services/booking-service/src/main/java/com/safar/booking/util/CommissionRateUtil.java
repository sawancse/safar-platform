package com.safar.booking.util;

import java.math.BigDecimal;

public class CommissionRateUtil {

    private CommissionRateUtil() {}

    public static BigDecimal getRate(String tier, String bookingType) {
        if ("AASHRAY".equalsIgnoreCase(bookingType)) return BigDecimal.ZERO;
        if ("MEDICAL".equalsIgnoreCase(bookingType)) return new BigDecimal("0.08");
        return switch (tier != null ? tier.toUpperCase() : "STARTER") {
            case "PRO" -> new BigDecimal("0.12");
            case "COMMERCIAL" -> new BigDecimal("0.10");
            default -> new BigDecimal("0.18"); // STARTER
        };
    }
}
