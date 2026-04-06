package com.safar.booking.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
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
    @CircuitBreaker(name = "listingService", fallbackMethod = "isAvailableFallback")
    @Retry(name = "listingService")
    public boolean isAvailable(UUID listingId, LocalDateTime checkIn, LocalDateTime checkOut) {
        @SuppressWarnings("unchecked")
        Map<String, Object> listing = fetchListing(listingId);
        if (listing == null) return false;
        String status = (String) listing.get("status");
        return "VERIFIED".equals(status);
    }

    public boolean isAvailableFallback(UUID listingId, LocalDateTime checkIn, LocalDateTime checkOut, Throwable t) {
        log.warn("Could not check availability for listing {}: {}", listingId, t.getMessage());
        return false;
    }

    @Override
    @CircuitBreaker(name = "listingService", fallbackMethod = "getBasePricePaiseFallback")
    @Retry(name = "listingService")
    public long getBasePricePaise(UUID listingId) {
        @SuppressWarnings("unchecked")
        Map<String, Object> listing = fetchListing(listingId);
        if (listing == null) return 0L;
        Object price = listing.get("basePricePaise");
        if (price instanceof Number) return ((Number) price).longValue();
        return 0L;
    }

    public long getBasePricePaiseFallback(UUID listingId, Throwable t) {
        log.warn("Could not get price for listing {}: {}", listingId, t.getMessage());
        return 0L;
    }

    @Override
    @CircuitBreaker(name = "listingService", fallbackMethod = "getHostIdFallback")
    @Retry(name = "listingService")
    public UUID getHostId(UUID listingId) {
        @SuppressWarnings("unchecked")
        Map<String, Object> listing = fetchListing(listingId);
        if (listing == null || listing.get("hostId") == null) return null;
        return UUID.fromString((String) listing.get("hostId"));
    }

    public UUID getHostIdFallback(UUID listingId, Throwable t) {
        log.warn("Could not get hostId for listing {}: {}", listingId, t.getMessage());
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    @CircuitBreaker(name = "listingService", fallbackMethod = "getGroupListingIdsFallback")
    @Retry(name = "listingService")
    public List<UUID> getGroupListingIds(UUID groupId) {
        Map<String, Object> group = restTemplate.getForObject(
                listingServiceUrl + "/api/v1/listing-groups/" + groupId, Map.class);
        if (group == null || group.get("listingIds") == null) return Collections.emptyList();
        List<String> ids = (List<String>) group.get("listingIds");
        return ids.stream().map(UUID::fromString).toList();
    }

    public List<UUID> getGroupListingIdsFallback(UUID groupId, Throwable t) {
        log.warn("Could not get group listing IDs for group {}: {}", groupId, t.getMessage());
        return Collections.emptyList();
    }

    @Override
    @SuppressWarnings("unchecked")
    @CircuitBreaker(name = "listingService", fallbackMethod = "getGroupBundleDiscountPctFallback")
    @Retry(name = "listingService")
    public int getGroupBundleDiscountPct(UUID groupId) {
        Map<String, Object> group = restTemplate.getForObject(
                listingServiceUrl + "/api/v1/listing-groups/" + groupId, Map.class);
        if (group == null || group.get("bundleDiscountPct") == null) return 0;
        Object pct = group.get("bundleDiscountPct");
        if (pct instanceof Number) return ((Number) pct).intValue();
        return 0;
    }

    public int getGroupBundleDiscountPctFallback(UUID groupId, Throwable t) {
        log.warn("Could not get bundle discount for group {}: {}", groupId, t.getMessage());
        return 0;
    }

    @Override
    @CircuitBreaker(name = "listingService", fallbackMethod = "getCityFallback")
    @Retry(name = "listingService")
    public String getCity(UUID listingId) {
        @SuppressWarnings("unchecked")
        Map<String, Object> listing = fetchListing(listingId);
        if (listing == null || listing.get("city") == null) return null;
        return (String) listing.get("city");
    }

    public String getCityFallback(UUID listingId, Throwable t) {
        log.warn("Could not get city for listing {}: {}", listingId, t.getMessage());
        return null;
    }

    @Override
    @CircuitBreaker(name = "listingService", fallbackMethod = "getListingTitleFallback")
    @Retry(name = "listingService")
    public String getListingTitle(UUID listingId) {
        @SuppressWarnings("unchecked")
        Map<String, Object> listing = fetchListing(listingId);
        if (listing == null || listing.get("title") == null) return "Unknown";
        return (String) listing.get("title");
    }

    public String getListingTitleFallback(UUID listingId, Throwable t) {
        log.warn("Could not get title for listing {}: {}", listingId, t.getMessage());
        return "Unknown";
    }

    @Override
    public String getListingPhotoUrl(UUID listingId) {
        try {
            Map<String, Object> listing = fetchListing(listingId);
            if (listing == null) return null;
            return listing.get("primaryPhotoUrl") != null ? (String) listing.get("primaryPhotoUrl") : null;
        } catch (Exception e) { return null; }
    }

    @Override
    public String getListingAddress(UUID listingId) {
        try {
            Map<String, Object> listing = fetchListing(listingId);
            if (listing == null) return null;
            return listing.get("address") != null ? (String) listing.get("address") : null;
        } catch (Exception e) { return null; }
    }

    @Override
    public String getHostName(UUID listingId) {
        try {
            Map<String, Object> listing = fetchListing(listingId);
            if (listing == null) return null;
            return listing.get("hostName") != null ? (String) listing.get("hostName") : null;
        } catch (Exception e) { return null; }
    }

    @Override
    @CircuitBreaker(name = "listingService", fallbackMethod = "getCleaningFeePaiseFallback")
    @Retry(name = "listingService")
    public long getCleaningFeePaise(UUID listingId) {
        @SuppressWarnings("unchecked")
        Map<String, Object> listing = fetchListing(listingId);
        if (listing == null) return 0L;
        Object fee = listing.get("cleaningFeePaise");
        if (fee instanceof Number) return ((Number) fee).longValue();
        return 0L;
    }

    public long getCleaningFeePaiseFallback(UUID listingId, Throwable t) {
        log.warn("Could not get cleaning fee for listing {}: {}", listingId, t.getMessage());
        return 0L;
    }

    @Override
    @CircuitBreaker(name = "listingService", fallbackMethod = "getHostTierFallback")
    @Retry(name = "listingService")
    public String getHostTier(UUID hostId) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> sub = restTemplate.getForObject(
                    listingServiceUrl + "/api/v1/internal/subscriptions/tier/" + hostId, Map.class);
            if (sub != null && sub.get("tier") != null) return (String) sub.get("tier");
        } catch (Exception e) {
            log.debug("Could not fetch host tier for {}, defaulting to STARTER", hostId);
        }
        return "STARTER";
    }

    public String getHostTierFallback(UUID hostId, Throwable t) {
        log.warn("Could not get host tier for {}: {}, defaulting to STARTER", hostId, t.getMessage());
        return "STARTER";
    }

    @Override
    @CircuitBreaker(name = "listingService", fallbackMethod = "getPricingUnitFallback")
    @Retry(name = "listingService")
    public String getPricingUnit(UUID listingId) {
        @SuppressWarnings("unchecked")
        Map<String, Object> listing = fetchListing(listingId);
        if (listing == null || listing.get("pricingUnit") == null) return "NIGHT";
        return (String) listing.get("pricingUnit");
    }

    public String getPricingUnitFallback(UUID listingId, Throwable t) {
        log.warn("Could not get pricing unit for listing {}: {}, defaulting to NIGHT", listingId, t.getMessage());
        return "NIGHT";
    }

    // ── Room type methods ───────────────────────────���───────────

    @Override
    @CircuitBreaker(name = "listingService", fallbackMethod = "getRoomTypePriceFallback")
    @Retry(name = "listingService")
    public long getRoomTypePrice(UUID listingId, UUID roomTypeId) {
        @SuppressWarnings("unchecked")
        Map<String, Object> rt = fetchRoomType(roomTypeId);
        if (rt == null) return 0L;
        Object price = rt.get("basePricePaise");
        if (price instanceof Number) return ((Number) price).longValue();
        return 0L;
    }

    public long getRoomTypePriceFallback(UUID listingId, UUID roomTypeId, Throwable t) {
        log.warn("Could not get room type price for {}: {}", roomTypeId, t.getMessage());
        return 0L;
    }

    @Override
    @CircuitBreaker(name = "listingService", fallbackMethod = "getRoomTypeNameFallback")
    @Retry(name = "listingService")
    public String getRoomTypeName(UUID listingId, UUID roomTypeId) {
        @SuppressWarnings("unchecked")
        Map<String, Object> rt = fetchRoomType(roomTypeId);
        if (rt == null || rt.get("name") == null) return null;
        return (String) rt.get("name");
    }

    public String getRoomTypeNameFallback(UUID listingId, UUID roomTypeId, Throwable t) {
        log.warn("Could not get room type name for {}: {}", roomTypeId, t.getMessage());
        return null;
    }

    @Override
    @CircuitBreaker(name = "listingService", fallbackMethod = "blockDatesFallback")
    @Retry(name = "listingService")
    public void blockDates(UUID listingId, LocalDate from, LocalDate to) {
        restTemplate.postForObject(
                listingServiceUrl + "/api/v1/internal/listings/" + listingId + "/block-dates",
                Map.of("fromDate", from.toString(), "toDate", to.toString()),
                Void.class);
    }

    public void blockDatesFallback(UUID listingId, LocalDate from, LocalDate to, Throwable t) {
        log.warn("Could not block dates for listing {}: {}", listingId, t.getMessage());
    }

    @Override
    @CircuitBreaker(name = "listingService", fallbackMethod = "unblockDatesFallback")
    @Retry(name = "listingService")
    public void unblockDates(UUID listingId, LocalDate from, LocalDate to) {
        restTemplate.postForObject(
                listingServiceUrl + "/api/v1/internal/listings/" + listingId + "/unblock-dates",
                Map.of("fromDate", from.toString(), "toDate", to.toString()),
                Void.class);
    }

    public void unblockDatesFallback(UUID listingId, LocalDate from, LocalDate to, Throwable t) {
        log.warn("Could not unblock dates for listing {}: {}", listingId, t.getMessage());
    }

    @Override
    @CircuitBreaker(name = "listingService", fallbackMethod = "decrementRoomTypeAvailabilityFallback")
    @Retry(name = "listingService")
    public void decrementRoomTypeAvailability(UUID roomTypeId, LocalDate from, LocalDate to, int count) {
        restTemplate.postForObject(
                listingServiceUrl + "/api/v1/internal/room-types/" + roomTypeId + "/decrement",
                Map.of("fromDate", from.toString(), "toDate", to.toString(), "count", count),
                Void.class);
    }

    public void decrementRoomTypeAvailabilityFallback(UUID roomTypeId, LocalDate from, LocalDate to, int count, Throwable t) {
        log.warn("Could not decrement room type availability for {}: {}", roomTypeId, t.getMessage());
    }

    @Override
    @CircuitBreaker(name = "listingService", fallbackMethod = "incrementRoomTypeAvailabilityFallback")
    @Retry(name = "listingService")
    public void incrementRoomTypeAvailability(UUID roomTypeId, LocalDate from, LocalDate to, int count) {
        restTemplate.postForObject(
                listingServiceUrl + "/api/v1/internal/room-types/" + roomTypeId + "/increment",
                Map.of("fromDate", from.toString(), "toDate", to.toString(), "count", count),
                Void.class);
    }

    public void incrementRoomTypeAvailabilityFallback(UUID roomTypeId, LocalDate from, LocalDate to, int count, Throwable t) {
        log.warn("Could not increment room type availability for {}: {}", roomTypeId, t.getMessage());
    }

    // ── PG/Hotel listing type support ──────────────────────────

    @Override
    @CircuitBreaker(name = "listingService", fallbackMethod = "getListingTypeFallback")
    @Retry(name = "listingService")
    public String getListingType(UUID listingId) {
        @SuppressWarnings("unchecked")
        Map<String, Object> listing = fetchListing(listingId);
        if (listing == null || listing.get("type") == null) return null;
        return (String) listing.get("type");
    }

    public String getListingTypeFallback(UUID listingId, Throwable t) {
        log.warn("Could not get listing type for {}: {}", listingId, t.getMessage());
        return null;
    }

    @Override
    @CircuitBreaker(name = "listingService", fallbackMethod = "getNoticePeriodDaysFallback")
    @Retry(name = "listingService")
    public Integer getNoticePeriodDays(UUID listingId) {
        @SuppressWarnings("unchecked")
        Map<String, Object> listing = fetchListing(listingId);
        if (listing == null) return null;
        Object val = listing.get("noticePeriodDays");
        if (val instanceof Number) return ((Number) val).intValue();
        return null;
    }

    public Integer getNoticePeriodDaysFallback(UUID listingId, Throwable t) {
        log.warn("Could not get notice period for {}: {}", listingId, t.getMessage());
        return null;
    }

    @Override
    @CircuitBreaker(name = "listingService", fallbackMethod = "getSecurityDepositPaiseFallback")
    @Retry(name = "listingService")
    public Long getSecurityDepositPaise(UUID listingId) {
        @SuppressWarnings("unchecked")
        Map<String, Object> listing = fetchListing(listingId);
        if (listing == null) return null;
        Object val = listing.get("securityDepositPaise");
        if (val instanceof Number) return ((Number) val).longValue();
        return null;
    }

    public Long getSecurityDepositPaiseFallback(UUID listingId, Throwable t) {
        log.warn("Could not get security deposit for {}: {}", listingId, t.getMessage());
        return null;
    }

    // ── Validation methods (new) ─────────────────────────────────

    @Override
    @CircuitBreaker(name = "listingService", fallbackMethod = "checkAvailabilityFallback")
    @Retry(name = "listingService")
    public Map<String, Object> checkAvailability(UUID listingId, LocalDate checkIn, LocalDate checkOut) {
        @SuppressWarnings("unchecked")
        Map<String, Object> result = restTemplate.getForObject(
                listingServiceUrl + "/api/v1/internal/listings/" + listingId
                        + "/check-availability?checkIn=" + checkIn + "&checkOut=" + checkOut,
                Map.class);
        return result != null ? result : Map.of("available", false);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> checkAvailabilityFallback(UUID listingId, LocalDate checkIn, LocalDate checkOut, Throwable t) {
        log.warn("Could not check availability for listing {}: {}", listingId, t.getMessage());
        return Map.of("available", false);
    }

    @Override
    @CircuitBreaker(name = "listingService", fallbackMethod = "checkRoomTypeAvailabilityFallback")
    @Retry(name = "listingService")
    public Map<String, Object> checkRoomTypeAvailability(UUID roomTypeId, LocalDate checkIn, LocalDate checkOut) {
        @SuppressWarnings("unchecked")
        Map<String, Object> result = restTemplate.getForObject(
                listingServiceUrl + "/api/v1/internal/listings/room-types/" + roomTypeId
                        + "/check-availability?checkIn=" + checkIn + "&checkOut=" + checkOut,
                Map.class);
        return result != null ? result : Map.of("minAvailable", 0);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> checkRoomTypeAvailabilityFallback(UUID roomTypeId, LocalDate checkIn, LocalDate checkOut, Throwable t) {
        log.warn("Could not check room type availability for {}: {}", roomTypeId, t.getMessage());
        return Map.of("minAvailable", 0);
    }

    @Override
    public Integer getMaxGuests(UUID listingId) {
        Map<String, Object> listing = fetchListing(listingId);
        if (listing == null) return null;
        Object val = listing.get("maxGuests");
        return val instanceof Number ? ((Number) val).intValue() : null;
    }

    @Override
    public Integer getTotalRooms(UUID listingId) {
        Map<String, Object> listing = fetchListing(listingId);
        if (listing == null) return null;
        Object val = listing.get("totalRooms");
        return val instanceof Number ? ((Number) val).intValue() : null;
    }

    @Override
    public Boolean getPetFriendly(UUID listingId) {
        Map<String, Object> listing = fetchListing(listingId);
        if (listing == null) return null;
        Object val = listing.get("petFriendly");
        return val instanceof Boolean ? (Boolean) val : null;
    }

    @Override
    public Integer getMaxPets(UUID listingId) {
        Map<String, Object> listing = fetchListing(listingId);
        if (listing == null) return null;
        Object val = listing.get("maxPets");
        return val instanceof Number ? ((Number) val).intValue() : null;
    }

    @Override
    public Integer getRoomTypeMaxGuests(UUID roomTypeId) {
        Map<String, Object> rt = fetchRoomType(roomTypeId);
        if (rt == null) return null;
        Object val = rt.get("maxGuests");
        return val instanceof Number ? ((Number) val).intValue() : null;
    }

    // ── Room type inclusions ────────────────────────────────────

    @Override
    @SuppressWarnings("unchecked")
    @CircuitBreaker(name = "listingService", fallbackMethod = "getRoomTypeInclusionsFallback")
    @Retry(name = "listingService")
    public List<Map<String, Object>> getRoomTypeInclusions(UUID roomTypeId) {
        List<Map<String, Object>> result = restTemplate.getForObject(
                listingServiceUrl + "/api/v1/internal/room-types/" + roomTypeId + "/inclusions",
                List.class);
        return result != null ? result : Collections.emptyList();
    }

    public List<Map<String, Object>> getRoomTypeInclusionsFallback(UUID roomTypeId, Throwable t) {
        log.warn("Could not get inclusions for room type {}: {}", roomTypeId, t.getMessage());
        return Collections.emptyList();
    }

    @Override
    public Map<String, Object> getRoomTypeInfo(UUID roomTypeId) {
        Map<String, Object> rt = fetchRoomType(roomTypeId);
        return rt != null ? rt : Map.of();
    }

    @Override
    public Long getRoomTypeSecurityDepositPaise(UUID roomTypeId) {
        Map<String, Object> rt = fetchRoomType(roomTypeId);
        if (rt == null) return null;
        Object val = rt.get("securityDepositPaise");
        if (val instanceof Number) return ((Number) val).longValue();
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchRoomType(UUID roomTypeId) {
        try {
            return restTemplate.getForObject(
                    listingServiceUrl + "/api/v1/internal/room-types/" + roomTypeId, Map.class);
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchListing(UUID listingId) {
        try {
            return restTemplate.getForObject(
                    listingServiceUrl + "/api/v1/listings/" + listingId, Map.class);
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        }
    }
}
