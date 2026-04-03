package com.safar.search.kafka;

import com.safar.search.document.BuilderProjectDocument;
import com.safar.search.document.SalePropertyDocument;
import com.safar.search.service.BuilderProjectSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class BuilderProjectIndexConsumer {

    private final BuilderProjectSearchService searchService;

    @KafkaListener(topics = "builder.project.indexed", groupId = "search-service")
    @SuppressWarnings("unchecked")
    public void onProjectIndexed(Map<String, Object> payload) {
        try {
            List<String> photos = payload.get("photos") != null ? (List<String>) payload.get("photos") : List.of();
            SalePropertyDocument.GeoPoint location = null;
            if (payload.get("lat") != null && payload.get("lng") != null) {
                location = new SalePropertyDocument.GeoPoint(
                        toDouble(payload.get("lat")), toDouble(payload.get("lng")));
            }

            BuilderProjectDocument doc = BuilderProjectDocument.builder()
                    .id(str(payload.get("id")))
                    .projectName(str(payload.get("projectName")))
                    .builderName(str(payload.get("builderName")))
                    .description(str(payload.get("description")))
                    .city(str(payload.get("city")))
                    .locality(str(payload.get("locality")))
                    .state(str(payload.get("state")))
                    .address(str(payload.get("address")))
                    .location(location)
                    .projectStatus(str(payload.get("projectStatus")))
                    .constructionProgressPercent(toInt(payload.get("constructionProgressPercent")))
                    .minPricePaise(toLong(payload.get("minPricePaise")))
                    .maxPricePaise(toLong(payload.get("maxPricePaise")))
                    .minBhk(toInt(payload.get("minBhk")))
                    .maxBhk(toInt(payload.get("maxBhk")))
                    .minAreaSqft(toInt(payload.get("minAreaSqft")))
                    .maxAreaSqft(toInt(payload.get("maxAreaSqft")))
                    .totalUnits(toInt(payload.get("totalUnits")))
                    .availableUnits(toInt(payload.get("availableUnits")))
                    .totalTowers(toInt(payload.get("totalTowers")))
                    .reraId(str(payload.get("reraId")))
                    .reraVerified(toBool(payload.get("reraVerified")))
                    .amenities(payload.get("amenities") != null ? (List<String>) payload.get("amenities") : null)
                    .bankApprovals(payload.get("bankApprovals") != null ? (List<String>) payload.get("bankApprovals") : null)
                    .primaryPhotoUrl(photos.isEmpty() ? null : photos.get(0))
                    .verified(toBool(payload.get("verified")))
                    .viewsCount(toInt(payload.get("viewsCount")))
                    .indexedAt(OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                    .build();

            searchService.index(doc);
            log.info("Indexed builder project: {}", doc.getId());
        } catch (Exception e) {
            log.error("Failed to index builder project: {}", e.getMessage(), e);
        }
    }

    private String str(Object o) { return o != null ? o.toString() : null; }
    private Long toLong(Object o) { return o instanceof Number n ? n.longValue() : null; }
    private Integer toInt(Object o) { return o instanceof Number n ? n.intValue() : null; }
    private Double toDouble(Object o) { return o instanceof Number n ? n.doubleValue() : null; }
    private Boolean toBool(Object o) { return o instanceof Boolean b ? b : null; }
}
