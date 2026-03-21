package com.safar.search.service;

import com.safar.search.document.ListingDocument;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class ListingServiceClientImpl implements ListingServiceClient {

    private final RestTemplate restTemplate;

    public ListingServiceClientImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Value("${services.listing-service.url}")
    private String listingServiceUrl;

    @Override
    @SuppressWarnings("unchecked")
    @CircuitBreaker(name = "listingService", fallbackMethod = "getListingDocumentFallback")
    @Retry(name = "listingService")
    public ListingDocument getListingDocument(UUID listingId) {
        Map<String, Object> listing = restTemplate.getForObject(
                listingServiceUrl + "/api/v1/listings/" + listingId, Map.class);
        if (listing == null) return null;

        Double lat = toDouble(listing.get("lat"));
        Double lng = toDouble(listing.get("lng"));
        GeoPoint location = (lat != null && lng != null) ? new GeoPoint(lat, lng) : null;

        Object price = listing.get("basePricePaise");
        Long basePricePaise = (price instanceof Number) ? ((Number) price).longValue() : null;

        return ListingDocument.builder()
                .id(listingId.toString())
                .title((String) listing.get("title"))
                .description((String) listing.get("description"))
                .type((String) listing.get("type"))
                .commercialCategory((String) listing.get("commercialCategory"))
                .city((String) listing.get("city"))
                .state((String) listing.get("state"))
                .pincode((String) listing.get("pincode"))
                .address(buildAddress(listing))
                .location(location)
                .basePricePaise(basePricePaise)
                .pricingUnit((String) listing.get("pricingUnit"))
                .maxGuests(toInt(listing.get("maxGuests")))
                .amenities((List<String>) listing.get("amenities"))
                .isVerified("VERIFIED".equals(listing.get("status")))
                .instantBook(Boolean.TRUE.equals(listing.get("instantBook")))
                .petFriendly(Boolean.TRUE.equals(listing.get("petFriendly")))
                .bedrooms(toInt(listing.get("bedrooms")))
                .bathrooms(toInt(listing.get("bathrooms")))
                .aiPricingEnabled(false)
                .isRemoteCertified(false)
                .starRating(toInt(listing.get("starRating")))
                .cancellationPolicy((String) listing.get("cancellationPolicy"))
                .mealPlan((String) listing.get("mealPlan"))
                .bedTypes((List<String>) listing.get("bedTypes"))
                .accessibilityFeatures((List<String>) listing.get("accessibilityFeatures"))
                .freeCancellation(Boolean.TRUE.equals(listing.get("freeCancellation")))
                .noPrepayment(Boolean.TRUE.equals(listing.get("noPrepayment")))
                .aashrayReady(Boolean.TRUE.equals(listing.get("aashrayReady")))
                .medicalStay(Boolean.TRUE.equals(listing.get("medicalStay")))
                .hospitalNames((List<String>) listing.get("hospitalNames"))
                .medicalSpecialties((List<String>) listing.get("medicalSpecialties"))
                .procedureNames((List<String>) listing.get("procedureNames"))
                // PG/Co-living fields
                .occupancyType((String) listing.get("occupancyType"))
                .foodType((String) listing.get("foodType"))
                .securityDepositPaise(toLong(listing.get("securityDepositPaise")))
                // Hotel fields
                .frontDesk24h(Boolean.TRUE.equals(listing.get("frontDesk24h")))
                .cleaningFeePaise(toLong(listing.get("cleaningFeePaise")))
                .visibilityBoostPercent(toInt(listing.get("visibilityBoostPercent")))
                .preferredPartner(Boolean.TRUE.equals(listing.get("preferredPartner")))
                .primaryPhotoUrl((String) listing.get("primaryPhotoUrl"))
                .indexedAt(LocalDateTime.now())
                .build();
    }

    public ListingDocument getListingDocumentFallback(UUID listingId, Throwable t) {
        log.warn("Could not fetch listing document for {}: {}", listingId, t.getMessage());
        return null;
    }

    private Double toDouble(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).doubleValue();
        try { return Double.parseDouble(val.toString()); } catch (Exception e) { return null; }
    }

    private Integer toInt(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).intValue();
        try { return Integer.parseInt(val.toString()); } catch (Exception e) { return null; }
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).longValue();
        try { return Long.parseLong(val.toString()); } catch (Exception e) { return null; }
    }

    private String buildAddress(Map<String, Object> listing) {
        StringBuilder sb = new StringBuilder();
        String a1 = (String) listing.get("addressLine1");
        String a2 = (String) listing.get("addressLine2");
        String city = (String) listing.get("city");
        String state = (String) listing.get("state");
        String pincode = (String) listing.get("pincode");
        if (a1 != null) sb.append(a1).append(" ");
        if (a2 != null) sb.append(a2).append(" ");
        if (city != null) sb.append(city).append(" ");
        if (state != null) sb.append(state).append(" ");
        if (pincode != null) sb.append(pincode);
        return sb.toString().trim();
    }
}
