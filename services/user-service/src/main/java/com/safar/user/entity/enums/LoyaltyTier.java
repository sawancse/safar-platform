package com.safar.user.entity.enums;

public enum LoyaltyTier {
    BRONZE,     // 0 stays — no discount
    SILVER,     // 2+ stays — 5% discount on select properties
    GOLD,       // 5+ stays — 10% discount + free cancellation
    PLATINUM    // 15+ stays — 15% discount + free cancellation + room upgrade
}
