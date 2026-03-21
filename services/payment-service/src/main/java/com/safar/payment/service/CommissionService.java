package com.safar.payment.service;

import com.safar.payment.dto.CommissionBreakdown;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class CommissionService {

    public static final BigDecimal STARTER_RATE = new BigDecimal("0.18");
    public static final BigDecimal PRO_RATE = new BigDecimal("0.12");
    public static final BigDecimal COMMERCIAL_RATE = new BigDecimal("0.10");
    public static final BigDecimal MEDICAL_RATE = new BigDecimal("0.08");
    public static final BigDecimal AASHRAY_RATE = BigDecimal.ZERO;

    public BigDecimal getCommissionRate(String tier, String bookingType) {
        if ("AASHRAY".equals(bookingType)) return AASHRAY_RATE;
        if ("MEDICAL".equals(bookingType)) return MEDICAL_RATE;

        return switch (tier != null ? tier.toUpperCase() : "STARTER") {
            case "PRO" -> PRO_RATE;
            case "COMMERCIAL" -> COMMERCIAL_RATE;
            default -> STARTER_RATE;
        };
    }

    public CommissionBreakdown calculate(long accommodationPaise, long treatmentPaise,
                                          String tier, String bookingType) {
        BigDecimal rate = getCommissionRate(tier, bookingType);
        // Commission only on accommodation, never on medical treatment
        long commissionablePaise = "MEDICAL".equals(bookingType)
                ? accommodationPaise
                : (accommodationPaise + treatmentPaise);
        long commissionPaise = BigDecimal.valueOf(commissionablePaise)
                .multiply(rate)
                .setScale(0, RoundingMode.FLOOR) // round in platform's favor
                .longValue();
        long hostPayoutPaise = accommodationPaise - commissionPaise;

        return new CommissionBreakdown(rate, commissionPaise, hostPayoutPaise,
                treatmentPaise, accommodationPaise);
    }
}
