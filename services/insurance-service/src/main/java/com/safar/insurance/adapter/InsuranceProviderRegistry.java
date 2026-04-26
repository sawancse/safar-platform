package com.safar.insurance.adapter;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Slf4j
public class InsuranceProviderRegistry {

    private final List<InsuranceProviderAdapter> adapters;
    private final Map<InsuranceProvider, InsuranceProviderAdapter> byType =
            new EnumMap<>(InsuranceProvider.class);

    @Value("${insurance.providers.primary:ACKO}")
    private String primaryName;

    public InsuranceProviderRegistry(List<InsuranceProviderAdapter> adapters) {
        this.adapters = adapters;
    }

    @PostConstruct
    void init() {
        for (InsuranceProviderAdapter a : adapters) {
            InsuranceProviderAdapter prev = byType.put(a.providerType(), a);
            if (prev != null) {
                throw new IllegalStateException(
                        "Duplicate InsuranceProviderAdapter for " + a.providerType());
            }
        }
        log.info("Insurance provider adapters registered: {} (primary={})", byType.keySet(), primaryName);
    }

    public InsuranceProviderAdapter get(InsuranceProvider type) {
        InsuranceProviderAdapter a = byType.get(type);
        if (a == null) {
            throw new IllegalStateException("No adapter registered for provider: " + type);
        }
        return a;
    }

    public List<InsuranceProviderAdapter> enabled() {
        return adapters.stream().filter(InsuranceProviderAdapter::isEnabled).toList();
    }

    public InsuranceProviderAdapter primary() {
        InsuranceProvider primary;
        try {
            primary = InsuranceProvider.valueOf(primaryName.toUpperCase());
        } catch (Exception e) {
            throw new IllegalStateException(
                    "insurance.providers.primary is not a valid InsuranceProvider: " + primaryName);
        }
        return get(primary);
    }
}
