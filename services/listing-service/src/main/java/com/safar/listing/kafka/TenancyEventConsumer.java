package com.safar.listing.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safar.listing.service.RoomTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Listens for PG tenancy lifecycle events to update room occupancy.
 * - tenancy.created  → increment occupiedBeds (tenant moves in)
 * - tenancy.vacated  → decrement occupiedBeds + restore date availability (tenant moves out)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TenancyEventConsumer {

    private final RoomTypeService roomTypeService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "tenancy.created", groupId = "listing-tenancy-group")
    public void onTenancyCreated(String payload) {
        try {
            Map<String, Object> event = parseEvent(payload);
            UUID roomTypeId = extractUuid(event, "roomTypeId");
            if (roomTypeId == null) {
                log.debug("Tenancy created without roomTypeId, skipping occupancy update");
                return;
            }

            String sharingType = (String) event.getOrDefault("sharingType", "PRIVATE");
            roomTypeService.incrementOccupancy(roomTypeId, sharingType);
            log.info("Tenant moved in: occupancy incremented for room type {} (sharing={})", roomTypeId, sharingType);
        } catch (Exception e) {
            log.error("Failed to process tenancy.created event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "tenancy.vacated", groupId = "listing-tenancy-group")
    public void onTenancyVacated(String payload) {
        try {
            Map<String, Object> event = parseEvent(payload);
            UUID roomTypeId = extractUuid(event, "roomTypeId");
            if (roomTypeId == null) {
                log.debug("Tenancy vacated without roomTypeId, skipping occupancy update");
                return;
            }

            String sharingType = (String) event.getOrDefault("sharingType", "PRIVATE");
            LocalDate moveOutDate = event.get("moveOutDate") != null
                    ? LocalDate.parse(event.get("moveOutDate").toString())
                    : LocalDate.now();

            roomTypeService.decrementOccupancy(roomTypeId, sharingType, moveOutDate);
            log.info("Tenant vacated: occupancy decremented for room type {} (sharing={}, moveOut={})",
                    roomTypeId, sharingType, moveOutDate);
        } catch (Exception e) {
            log.error("Failed to process tenancy.vacated event: {}", e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseEvent(String payload) throws Exception {
        return objectMapper.readValue(payload, Map.class);
    }

    private UUID extractUuid(Map<String, Object> event, String field) {
        Object value = event.get(field);
        if (value == null) return null;
        return UUID.fromString(value.toString());
    }
}
