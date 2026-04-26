package com.safar.chef.entity.enums;

/**
 * Commission tier for service-leg vendors (Hybrid C monetization).
 *
 * Tier *names* are universal — STARTER → PRO → COMMERCIAL — but the actual
 * commission rates and promotion thresholds vary by service_type, so a cake
 * baker (high margin) and a staff agency (thin margin) can sit at the same
 * tier and pay different commissions. Lookups go through {@code
 * CommissionRateConfig.rateFor(serviceType, tier)}.
 *
 * Tier is performance-based, NOT paid: vendors auto-promote on completed
 * booking count. Subscription plan is a separate dimension that can override
 * the commission rate.
 */
public enum CommissionTier {
    STARTER,
    PRO,
    COMMERCIAL
}
