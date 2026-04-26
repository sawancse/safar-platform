package com.safar.insurance.service;

import com.safar.insurance.adapter.*;
import com.safar.insurance.dto.InsuranceQuoteRequest;
import com.safar.insurance.dto.InsuranceQuoteResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Fan-out quote across all enabled insurance providers in parallel,
 * sort cheapest premium first. One provider's failure doesn't kill the
 * response — its quote just won't appear.
 *
 * Mirrors the FlightSearchService pattern.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InsuranceQuoteService {

    private final InsuranceProviderRegistry registry;

    public InsuranceQuoteResponse quote(InsuranceQuoteRequest req) {
        List<InsuranceProviderAdapter> adapters = registry.enabled();
        if (adapters.isEmpty()) {
            log.warn("No insurance provider adapters enabled — returning empty quotes");
            return new InsuranceQuoteResponse(List.of());
        }

        QuoteRequest providerReq = new QuoteRequest(
                req.tripOriginCode(), req.tripDestinationCode(),
                req.tripOriginCountry() != null ? req.tripOriginCountry() : "IN",
                req.tripDestinationCountry() != null ? req.tripDestinationCountry() : "IN",
                req.tripStartDate(), req.tripEndDate(),
                req.coverageType(), req.travellerAges()
        );

        List<CompletableFuture<Optional<QuoteResult>>> futures = adapters.stream()
                .map(a -> CompletableFuture.supplyAsync(() -> {
                    try {
                        long t = System.currentTimeMillis();
                        QuoteResult r = a.quote(providerReq);
                        log.info("{} returned quote {} {} in {} ms",
                                a.providerType(), r.premiumPaise(), r.currency(),
                                System.currentTimeMillis() - t);
                        return Optional.ofNullable(r);
                    } catch (Exception e) {
                        log.warn("{} quote failed: {}", a.providerType(), e.getMessage());
                        return Optional.<QuoteResult>empty();
                    }
                }))
                .toList();

        List<InsuranceQuoteResponse.Quote> merged = new ArrayList<>();
        for (CompletableFuture<Optional<QuoteResult>> f : futures) {
            try {
                f.get().ifPresent(r -> merged.add(toDto(r)));
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException ee) {
                log.warn("Quote future failed: {}", ee.getMessage());
            }
        }

        merged.sort(Comparator.comparingLong(InsuranceQuoteResponse.Quote::premiumPaise));
        return new InsuranceQuoteResponse(merged);
    }

    private static InsuranceQuoteResponse.Quote toDto(QuoteResult r) {
        return new InsuranceQuoteResponse.Quote(
                ProviderQuoteId.encode(r.provider(), r.providerQuoteToken()),
                r.provider().name(),
                r.premiumPaise(),
                r.sumInsuredPaise(),
                r.currency(),
                r.coverageHighlights(),
                r.fareRulesUrl()
        );
    }
}
