package com.safar.flight.service;

import com.safar.flight.adapter.FlightProviderAdapter;
import com.safar.flight.adapter.FlightProviderRegistry;
import com.safar.flight.dto.FlightSearchRequest;
import com.safar.flight.dto.FlightSearchResponse;
import com.safar.flight.dto.FlightSearchResponse.FlightOffer;
import com.safar.flight.entity.FlightSearchEvent;
import com.safar.flight.repository.FlightSearchEventRepository;
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
 *
 * Also captures every search as a {@link FlightSearchEvent} so the
 * AbandonedSearchDetector can drive recovery campaigns.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FlightSearchService {

    private final FlightProviderRegistry registry;
    private final FlightSearchEventRepository searchEventRepository;

    public FlightSearchResponse search(FlightSearchRequest request) {
        return search(request, null, null, null, null);
    }

    /**
     * Overload for the controller — captures user identity (when logged in)
     * and device id (for anonymous tracking) so the abandoned-search detector
     * has someone to send the reminder to.
     */
    public FlightSearchResponse search(FlightSearchRequest request,
                                       UUID userId,
                                       String deviceId,
                                       String contactEmail,
                                       String contactPhone) {
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

        // Fire-and-forget event capture — never let persistence fail block the response
        try {
            captureSearchEvent(request, merged, userId, deviceId, contactEmail, contactPhone);
        } catch (Exception e) {
            log.warn("Failed to persist FlightSearchEvent (non-fatal): {}", e.getMessage());
        }

        return new FlightSearchResponse(merged);
    }

    private void captureSearchEvent(FlightSearchRequest request,
                                    List<FlightOffer> results,
                                    UUID userId,
                                    String deviceId,
                                    String contactEmail,
                                    String contactPhone) {
        // Skip capture entirely if we have no identity to message later.
        if (userId == null && (deviceId == null || deviceId.isBlank())) {
            return;
        }
        Long cheapestPaise = results.isEmpty() ? null : results.get(0).pricePaise();
        String currency = results.isEmpty() ? "INR" : results.get(0).currency();

        FlightSearchEvent event = FlightSearchEvent.builder()
                .userId(userId)
                .deviceId(deviceId)
                .origin(request.origin())
                .destination(request.destination())
                .departureDate(request.departureDate())
                .returnDate(request.returnDate())
                .paxCount(request.passengers() != null ? request.passengers() : 1)
                .cabinClass(request.cabinClass())
                .cheapestFarePaise(cheapestPaise)
                .currency(currency)
                .contactEmail(contactEmail)
                .contactPhone(contactPhone)
                .build();
        searchEventRepository.save(event);
    }
}
