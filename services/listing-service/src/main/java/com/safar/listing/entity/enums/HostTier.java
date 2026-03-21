package com.safar.listing.entity.enums;

public enum HostTier {
    STARTER(2),
    PRO(10),
    COMMERCIAL(Integer.MAX_VALUE);

    private final int maxListings;

    HostTier(int maxListings) {
        this.maxListings = maxListings;
    }

    public int getMaxListings() {
        return maxListings;
    }

    public static HostTier from(String tier) {
        if (tier == null) return STARTER;
        try {
            return valueOf(tier.toUpperCase());
        } catch (IllegalArgumentException e) {
            return STARTER;
        }
    }
}
