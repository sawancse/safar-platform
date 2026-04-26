package com.safar.flight.service;

import com.safar.flight.adapter.FlightProviderAdapter;
import com.safar.flight.adapter.FlightProviderRegistry;
import com.safar.flight.dto.FlightSearchRequest;
import com.safar.flight.dto.FlightSearchResponse;
import com.safar.flight.dto.FlightSearchResponse.FlightOffer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Fan-out search across every enabled {@link FlightProviderAdapter} in
 * parallel, merge results, sort cheapest-first. One adapter's failure does
 * not kill the response — its offers just won't appear.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FlightSearchService {

    private final FlightProviderRegistry registry;

    public FlightSearchResponse search(FlightSearchRequest request) {
        List<FlightProviderAdapter> adapters = registry.enabled();
        if (adapters.isEmpty()) {
            log.warn("No flight provider adapters enabled — returning empty results");
            return new FlightSearchResponse(List.of());
        }

        List<CompletableFuture<List<FlightOffer>>> futures = adapters.stream()
                .map(a -> CompletableFuture.supplyAsync(() -> {
                    try {
                        long t = System.currentTimeMillis();
                        List<FlightOffer> offers = a.search(request);
                        log.info("{} returned {} offers in {} ms",
                                a.providerType(), offers.size(), System.currentTimeMillis() - t);
                        return offers;
                    } catch (Exception e) {
                        log.warn("{} search failed: {}", a.providerType(), e.getMessage());
                        return Collections.<FlightOffer>emptyList();
                    }
                }))
                .toList();

        List<FlightOffer> merged = new ArrayList<>();
        for (CompletableFuture<List<FlightOffer>> f : futures) {
            try {
                merged.addAll(f.get());
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException ee) {
                log.warn("Adapter future failed: {}", ee.getMessage());
            }
        }

        merged.sort(Comparator.comparingLong(FlightOffer::pricePaise));
        return new FlightSearchResponse(merged);
    }
}
