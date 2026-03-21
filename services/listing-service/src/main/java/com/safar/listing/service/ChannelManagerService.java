package com.safar.listing.service;

import com.safar.listing.entity.ChannelManagerProperty;
import com.safar.listing.entity.ChannelMapping;
import com.safar.listing.entity.ChannelSyncLog;
import com.safar.listing.entity.Listing;
import com.safar.listing.entity.enums.ChannelStatus;
import com.safar.listing.entity.enums.MappingStatus;
import com.safar.listing.repository.ChannelManagerPropertyRepository;
import com.safar.listing.repository.ChannelMappingRepository;
import com.safar.listing.repository.ChannelSyncLogRepository;
import com.safar.listing.repository.ListingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChannelManagerService {

    private final ChannelManagerPropertyRepository propertyRepository;
    private final ChannelMappingRepository mappingRepository;
    private final ChannelSyncLogRepository syncLogRepository;
    private final ListingRepository listingRepository;
    private final RestTemplate restTemplate;

    @Value("${channex.api-url:https://app.channex.io/api/v1}")
    private String channexApiUrl;

    @Value("${channex.api-key:}")
    private String channexApiKey;

    @Transactional
    public ChannelManagerProperty connectProperty(UUID listingId) {
        if (propertyRepository.existsByListingId(listingId)) {
            throw new RuntimeException("Listing already connected to channel manager");
        }

        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new RuntimeException("Listing not found: " + listingId));

        // Create Channex property via API
        String channexPropertyId = createChannexProperty(listing);

        ChannelManagerProperty cmp = ChannelManagerProperty.builder()
                .listingId(listingId)
                .channexPropertyId(channexPropertyId)
                .status(ChannelStatus.CONNECTED)
                .connectedChannels("[]")
                .build();

        ChannelManagerProperty saved = propertyRepository.save(cmp);
        logSync(saved.getId(), "PUSH", "CONTENT", null, true, null, 1);
        log.info("Connected listing {} to channel manager", listingId);
        return saved;
    }

    @Transactional
    public void disconnectProperty(UUID listingId) {
        ChannelManagerProperty cmp = propertyRepository.findByListingId(listingId)
                .orElseThrow(() -> new RuntimeException("No channel manager connection for listing: " + listingId));
        cmp.setStatus(ChannelStatus.DISCONNECTED);
        propertyRepository.save(cmp);
        log.info("Disconnected listing {} from channel manager", listingId);
    }

    @Transactional
    public void syncRates(UUID listingId) {
        ChannelManagerProperty cmp = getConnectedProperty(listingId);
        try {
            pushRatesToChannex(cmp);
            cmp.setLastSyncAt(OffsetDateTime.now());
            propertyRepository.save(cmp);
            logSync(cmp.getId(), "PUSH", "RATE", null, true, null, 1);
        } catch (Exception e) {
            logSync(cmp.getId(), "PUSH", "RATE", null, false, e.getMessage(), 0);
            throw new RuntimeException("Rate sync failed: " + e.getMessage());
        }
    }

    @Transactional
    public void syncAvailability(UUID listingId) {
        ChannelManagerProperty cmp = getConnectedProperty(listingId);
        try {
            pushAvailabilityToChannex(cmp);
            cmp.setLastSyncAt(OffsetDateTime.now());
            propertyRepository.save(cmp);
            logSync(cmp.getId(), "PUSH", "AVAILABILITY", null, true, null, 1);
        } catch (Exception e) {
            logSync(cmp.getId(), "PUSH", "AVAILABILITY", null, false, e.getMessage(), 0);
            throw new RuntimeException("Availability sync failed: " + e.getMessage());
        }
    }

    @Transactional
    public int pullBookings(UUID listingId) {
        ChannelManagerProperty cmp = getConnectedProperty(listingId);
        try {
            int count = pullBookingsFromChannex(cmp);
            cmp.setLastSyncAt(OffsetDateTime.now());
            propertyRepository.save(cmp);
            logSync(cmp.getId(), "PULL", "BOOKING", null, true, null, count);
            return count;
        } catch (Exception e) {
            logSync(cmp.getId(), "PULL", "BOOKING", null, false, e.getMessage(), 0);
            throw new RuntimeException("Booking pull failed: " + e.getMessage());
        }
    }

    public ChannelManagerProperty getConnectionStatus(UUID listingId) {
        return propertyRepository.findByListingId(listingId)
                .orElseThrow(() -> new RuntimeException("No channel manager connection for listing: " + listingId));
    }

    public Page<ChannelSyncLog> getSyncLogs(UUID listingId, Pageable pageable) {
        ChannelManagerProperty cmp = getConnectionStatus(listingId);
        return syncLogRepository.findByChannelManagerPropertyIdOrderBySyncedAtDesc(cmp.getId(), pageable);
    }

    public List<ChannelMapping> getMappings(UUID listingId) {
        ChannelManagerProperty cmp = getConnectionStatus(listingId);
        return mappingRepository.findByChannelManagerPropertyId(cmp.getId());
    }

    @Transactional
    public ChannelMapping createMapping(ChannelMapping mapping) {
        return mappingRepository.save(mapping);
    }

    @Transactional
    public void deleteMapping(UUID mappingId) {
        mappingRepository.deleteById(mappingId);
    }

    // ─── Internal Methods ───

    private ChannelManagerProperty getConnectedProperty(UUID listingId) {
        ChannelManagerProperty cmp = propertyRepository.findByListingId(listingId)
                .orElseThrow(() -> new RuntimeException("No channel manager connection for listing: " + listingId));
        if (cmp.getStatus() == ChannelStatus.DISCONNECTED) {
            throw new RuntimeException("Channel manager is disconnected for listing: " + listingId);
        }
        return cmp;
    }

    private String createChannexProperty(Listing listing) {
        if (channexApiKey.isEmpty()) {
            log.warn("Channex API key not configured — using mock property ID");
            return "mock-channex-" + UUID.randomUUID().toString().substring(0, 8);
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + channexApiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = Map.of(
                    "title", listing.getTitle(),
                    "city", listing.getCity(),
                    "address", listing.getAddressLine1(),
                    "latitude", listing.getLat(),
                    "longitude", listing.getLng()
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    channexApiUrl + "/properties", request, Map.class);

            if (response.getStatusCode() == HttpStatus.CREATED && response.getBody() != null) {
                Map data = (Map) response.getBody().get("data");
                return data != null ? (String) data.get("id") : UUID.randomUUID().toString();
            }
        } catch (Exception e) {
            log.warn("Channex API call failed, using mock: {}", e.getMessage());
        }
        return "mock-channex-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private void pushRatesToChannex(ChannelManagerProperty cmp) {
        log.info("Pushing rates to Channex for property {}", cmp.getChannexPropertyId());
        // In production: PUT /rates with room type rates
    }

    private void pushAvailabilityToChannex(ChannelManagerProperty cmp) {
        log.info("Pushing availability to Channex for property {}", cmp.getChannexPropertyId());
        // In production: PUT /availability with date ranges
    }

    private int pullBookingsFromChannex(ChannelManagerProperty cmp) {
        log.info("Pulling bookings from Channex for property {}", cmp.getChannexPropertyId());
        // In production: GET /bookings?property_id=X&status=new
        return 0;
    }

    private void logSync(UUID propertyId, String direction, String syncType,
                          String channelName, boolean success, String error, int records) {
        syncLogRepository.save(ChannelSyncLog.builder()
                .channelManagerPropertyId(propertyId)
                .direction(direction)
                .syncType(syncType)
                .channelName(channelName)
                .success(success)
                .errorMessage(error)
                .recordsAffected(records)
                .build());
    }
}
