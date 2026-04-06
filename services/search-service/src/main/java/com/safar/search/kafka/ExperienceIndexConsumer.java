package com.safar.search.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safar.search.document.ExperienceDocument;
import com.safar.search.service.ExperienceSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExperienceIndexConsumer {

    private final ExperienceSearchService searchService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "experience.activated", groupId = "search-service")
    public void onExperienceActivated(String message) {
        try {
            JsonNode json = objectMapper.readTree(message);
            ExperienceDocument doc = mapToDocument(json);
            searchService.index(doc);
            log.info("Indexed experience: {}", doc.getId());
        } catch (Exception e) {
            log.error("Failed to index experience: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "experience.rejected", groupId = "search-service")
    public void onExperienceRejected(String message) {
        try {
            JsonNode json = objectMapper.readTree(message);
            String id = json.get("id").asText();
            searchService.delete(id);
            log.info("Deleted experience from index: {}", id);
        } catch (Exception e) {
            log.error("Failed to delete experience from index: {}", e.getMessage(), e);
        }
    }

    private ExperienceDocument mapToDocument(JsonNode j) {
        ExperienceDocument.GeoPoint location = null;
        if (j.has("meetingPointLat") && !j.get("meetingPointLat").isNull()
                && j.has("meetingPointLng") && !j.get("meetingPointLng").isNull()) {
            location = new ExperienceDocument.GeoPoint(
                    j.get("meetingPointLat").asDouble(),
                    j.get("meetingPointLng").asDouble()
            );
        }

        return ExperienceDocument.builder()
                .id(j.get("id").asText())
                .title(j.has("title") ? j.get("title").asText() : null)
                .description(j.has("description") ? j.get("description").asText() : null)
                .category(j.has("category") ? j.get("category").asText() : null)
                .city(j.has("city") ? j.get("city").asText() : null)
                .locationName(j.has("locationName") ? j.get("locationName").asText() : null)
                .location(location)
                .durationHours(j.has("durationHours") ? j.get("durationHours").asDouble() : null)
                .maxGuests(j.has("maxGuests") ? j.get("maxGuests").asInt() : null)
                .pricePaise(j.has("pricePaise") ? j.get("pricePaise").asLong() : null)
                .languagesSpoken(j.has("languagesSpoken") ? j.get("languagesSpoken").asText() : null)
                .mediaUrls(j.has("mediaUrls") ? j.get("mediaUrls").asText() : null)
                .cancellationPolicy(j.has("cancellationPolicy") ? j.get("cancellationPolicy").asText() : null)
                .isPrivate(j.has("isPrivate") ? j.get("isPrivate").asBoolean() : false)
                .minAge(j.has("minAge") && !j.get("minAge").isNull() ? j.get("minAge").asInt() : null)
                .rating(j.has("rating") && !j.get("rating").isNull() ? j.get("rating").asDouble() : null)
                .reviewCount(j.has("reviewCount") ? j.get("reviewCount").asInt() : 0)
                .hostId(j.has("hostId") ? j.get("hostId").asText() : null)
                .indexedAt(System.currentTimeMillis())
                .build();
    }
}
