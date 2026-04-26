package com.safar.flight.adapter;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Spring auto-injects every {@link FlightProviderAdapter} bean and indexes
 * them by {@link FlightProvider}. Centralizes routing for search + booking.
 */
@Component
@Slf4j
public class FlightProviderRegistry {

    private final List<FlightProviderAdapter> adapters;
    private final Map<FlightProvider, FlightProviderAdapter> byType = new EnumMap<>(FlightProvider.class);

    @Value("${flight.providers.primary:AMADEUS}")
    private String primaryProviderName;

    public FlightProviderRegistry(List<FlightProviderAdapter> adapters) {
        this.adapters = adapters;
    }

    @PostConstruct
    void init() {
        for (FlightProviderAdapter a : adapters) {
            FlightProviderAdapter prev = byType.put(a.providerType(), a);
            if (prev != null) {
                throw new IllegalStateException(
                        "Duplicate FlightProviderAdapter for " + a.providerType());
            }
        }
        log.info("Flight provider adapters registered: {} (primary={})",
                byType.keySet(), primaryProviderName);
    }

    public FlightProviderAdapter get(FlightProvider type) {
        FlightProviderAdapter a = byType.get(type);
        if (a == null) {
            throw new IllegalStateException("No adapter registered for provider: " + type);
        }
        return a;
    }

    /** All adapters that are currently enabled (creds present + flag on). */
    public List<FlightProviderAdapter> enabled() {
        return adapters.stream().filter(FlightProviderAdapter::isEnabled).toList();
    }

    /** All enabled adapters that can issue PNRs. */
    public List<FlightProviderAdapter> bookable() {
        return adapters.stream()
                .filter(FlightProviderAdapter::isEnabled)
                .filter(FlightProviderAdapter::canBook)
                .toList();
    }

    /** The configured primary bookable provider for new PNRs. */
    public FlightProviderAdapter primary() {
        FlightProvider primary;
        try {
            primary = FlightProvider.valueOf(primaryProviderName.toUpperCase());
        } catch (Exception e) {
            throw new IllegalStateException(
                    "flight.providers.primary is not a valid FlightProvider: " + primaryProviderName);
        }
        FlightProviderAdapter a = get(primary);
        if (!a.canBook()) {
            throw new IllegalStateException(
                    "Configured primary provider " + primary + " cannot book; pick a bookable one.");
        }
        return a;
    }
}
