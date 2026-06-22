package com.safar.listing.esign;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Picks the active eSign / e-Stamp provider by config (default sandbox). */
@Component
public class AgreementProviderResolver {

    private final Map<String, EsignProvider> esignByName;
    private final Map<String, EStampProvider> estampByName;

    @Value("${agreement.esign.provider:sandbox}")
    private String esignProviderName;
    @Value("${agreement.estamp.provider:sandbox}")
    private String estampProviderName;

    public AgreementProviderResolver(List<EsignProvider> esignProviders, List<EStampProvider> estampProviders) {
        this.esignByName = esignProviders.stream()
                .collect(Collectors.toMap(p -> p.name().toUpperCase(), Function.identity(), (a, b) -> a));
        this.estampByName = estampProviders.stream()
                .collect(Collectors.toMap(p -> p.name().toUpperCase(), Function.identity(), (a, b) -> a));
    }

    public EsignProvider esign() {
        EsignProvider p = esignByName.get(esignProviderName.toUpperCase());
        if (p == null) throw new IllegalStateException("No eSign provider configured for '" + esignProviderName + "'");
        return p;
    }

    public EStampProvider estamp() {
        EStampProvider p = estampByName.get(estampProviderName.toUpperCase());
        if (p == null) throw new IllegalStateException("No e-Stamp provider configured for '" + estampProviderName + "'");
        return p;
    }
}
