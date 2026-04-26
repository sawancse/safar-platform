package com.safar.services.service;

import com.safar.services.entity.CommissionRateConfigEntity;
import com.safar.services.entity.ServiceListing;
import com.safar.services.entity.enums.CommissionTier;
import com.safar.services.entity.enums.ServiceListingType;
import com.safar.services.repository.CommissionRateConfigRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DB-backed commission rate lookup. Caches the full rate table on startup
 * and refreshes every 5 minutes — admin edits in psql or via the future
 * admin endpoint pick up on the next refresh tick.
 *
 * Lookup precedence for a given (listing, tier):
 *   1. listing.commissionPctOverride (per-vendor negotiated rate)
 *   2. config table[serviceType, tier]
 *   3. defensive hardcoded fallback (in case the config table is empty)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CommissionRateService {

    private final CommissionRateConfigRepository repo;

    // (serviceType, tier) -> entity. Refreshed from DB every 5 minutes.
    private volatile Map<String, CommissionRateConfigEntity> cache = new HashMap<>();
    private volatile long cachedAtMs = 0L;
    private static final long CACHE_TTL_MS = 5 * 60_000L;

    @PostConstruct
    void init() {
        refreshCache();
    }

    /** Effective commission rate for this listing's current tier — applies override if set. */
    public BigDecimal effectiveRate(ServiceListing listing) {
        if (listing.getCommissionPctOverride() != null) {
            return listing.getCommissionPctOverride();
        }
        return rateFor(listing.getServiceType(), listing.getCommissionTier());
    }

    /** Looked-up rate for a (service_type, tier) — ignores per-vendor override. */
    public BigDecimal rateFor(String serviceType, String tier) {
        CommissionRateConfigEntity row = lookup(serviceType, tier);
        if (row != null) return row.getCommissionPct();
        return fallbackRate(tier);
    }

    /** Booking-count threshold to enter target tier for this service type. */
    public int thresholdFor(String serviceType, String tier) {
        CommissionRateConfigEntity row = lookup(serviceType, tier);
        if (row != null) return row.getPromotionThreshold();
        return fallbackThreshold(tier);
    }

    /**
     * Highest tier the vendor qualifies for given completed-booking count.
     * Used by TrustTierScheduler to auto-promote vendors nightly.
     */
    public CommissionTier qualifyingTier(ServiceListingType serviceType, int completedBookings) {
        return qualifyingTier(serviceType.name(), completedBookings);
    }

    public CommissionTier qualifyingTier(String serviceType, int completedBookings) {
        if (completedBookings >= thresholdFor(serviceType, CommissionTier.COMMERCIAL.name())) return CommissionTier.COMMERCIAL;
        if (completedBookings >= thresholdFor(serviceType, CommissionTier.PRO.name())) return CommissionTier.PRO;
        return CommissionTier.STARTER;
    }

    /** Admin endpoint helper — bumps cache so changes surface immediately. */
    @Transactional
    public CommissionRateConfigEntity saveRate(CommissionRateConfigEntity entity) {
        CommissionRateConfigEntity saved = repo.save(entity);
        refreshCache();
        return saved;
    }

    public List<CommissionRateConfigEntity> listForType(String serviceType) {
        return repo.findByServiceType(serviceType);
    }

    public List<CommissionRateConfigEntity> listAll() {
        return repo.findAll();
    }

    private CommissionRateConfigEntity lookup(String serviceType, String tier) {
        if (System.currentTimeMillis() - cachedAtMs > CACHE_TTL_MS) {
            refreshCache();
        }
        return cache.get(key(serviceType, tier));
    }

    private void refreshCache() {
        try {
            Map<String, CommissionRateConfigEntity> next = new HashMap<>();
            for (CommissionRateConfigEntity row : repo.findAll()) {
                next.put(key(row.getServiceType(), row.getTier()), row);
            }
            this.cache = next;
            this.cachedAtMs = System.currentTimeMillis();
        } catch (Exception e) {
            // Don't blow up on a transient DB hiccup — keep the old cache.
            log.warn("Failed to refresh commission rate cache: {}", e.getMessage());
        }
    }

    private static String key(String serviceType, String tier) {
        return serviceType + "/" + tier;
    }

    /** Defensive fallback if config table is empty (shouldn't happen post-V25 seed). */
    private BigDecimal fallbackRate(String tier) {
        return switch (tier) {
            case "PRO" -> new BigDecimal("12.00");
            case "COMMERCIAL" -> new BigDecimal("10.00");
            default -> new BigDecimal("18.00");
        };
    }

    private int fallbackThreshold(String tier) {
        return switch (tier) {
            case "PRO" -> 10;
            case "COMMERCIAL" -> 50;
            default -> 0;
        };
    }
}
