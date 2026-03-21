package com.safar.listing.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safar.listing.entity.ListingMedia;
import com.safar.listing.entity.enums.ModerationStatus;
import com.safar.listing.repository.ListingMediaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class MediaEventConsumer {

    private final ListingMediaRepository listingMediaRepository;
    private final ObjectMapper objectMapper;

    /**
     * Consumes media.uploaded events from the media-service.
     * Expected payload: {"mediaId":"...", "listingId":"...", "cdnUrl":"...", "type":"PHOTO|VIDEO|3D",
     *                    "s3Key":"...", "isPrimary":false, "sortOrder":0}
     */
    @KafkaListener(topics = "media.uploaded", groupId = "listing-media-group")
    public void onMediaUploaded(String message) {
        try {
            JsonNode json = objectMapper.readTree(message);
            UUID listingId = UUID.fromString(json.get("listingId").asText());
            String cdnUrl  = json.has("cdnUrl")  ? json.get("cdnUrl").asText()  : null;
            String s3Key   = json.has("s3Key")   ? json.get("s3Key").asText()   : "";
            String type    = json.has("type")    ? json.get("type").asText()    : "PHOTO";
            String modStatus = json.has("moderationStatus") ? json.get("moderationStatus").asText() : "PENDING";
            boolean primary = json.has("isPrimary") && json.get("isPrimary").asBoolean();
            int sortOrder  = json.has("sortOrder") ? json.get("sortOrder").asInt() : 0;

            // Check if a record already exists (media-service may send duplicate events)
            if (cdnUrl != null && listingMediaRepository.existsByListingIdAndCdnUrl(listingId, cdnUrl)) {
                log.debug("Duplicate media.uploaded event ignored for listing {}", listingId);
                return;
            }

            String category = json.has("category") ? json.get("category").asText(null) : null;

            ListingMedia media = ListingMedia.builder()
                    .listingId(listingId)
                    .type(com.safar.listing.entity.enums.MediaType.valueOf(type))
                    .s3Key(s3Key)
                    .cdnUrl(cdnUrl)
                    .isPrimary(primary)
                    .sortOrder(sortOrder)
                    .category(category)
                    .moderationStatus(ModerationStatus.valueOf(modStatus))
                    .build();

            listingMediaRepository.save(media);
            log.info("Saved media record for listing {} from Kafka event", listingId);
        } catch (Exception e) {
            log.error("Failed to process media.uploaded event: {} — {}", message, e.getMessage());
        }
    }
}
