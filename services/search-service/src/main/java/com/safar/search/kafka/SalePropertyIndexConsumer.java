package com.safar.search.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safar.search.document.SalePropertyDocument;
import com.safar.search.service.SalePropertySearchService;
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
public class SalePropertyIndexConsumer {

    private final SalePropertySearchService searchService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "sale.property.indexed", groupId = "search-service")
    @SuppressWarnings("unchecked")
    public void onSalePropertyIndexed(String message) {
        try {
            Map<String, Object> payload = objectMapper.readValue(message, Map.class);
            SalePropertyDocument doc = mapToDocument(payload);
            searchService.index(doc);
            log.info("Indexed sale property: {}", doc.getId());
        } catch (Exception e) {
            log.error("Failed to index sale property: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "sale.property.deleted", groupId = "search-service")
    public void onSalePropertyDeleted(String propertyId) {
        try {
            // propertyId might be a UUID string or JSON-wrapped
            String id = propertyId.replace("\"", "").trim();
            searchService.delete(id);
            log.info("Deleted sale property from index: {}", id);
        } catch (Exception e) {
            log.error("Failed to delete sale property from index: {}", e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private SalePropertyDocument mapToDocument(Map<String, Object> p) {
        String address = java.util.stream.Stream.of(
                p.get("addressLine1"), p.get("addressLine2"), p.get("locality")
        ).filter(java.util.Objects::nonNull).map(Object::toString).collect(java.util.stream.Collectors.joining(" ")).trim();

        List<String> photos = p.get("photos") != null ? (List<String>) p.get("photos") : List.of();
        String primaryPhoto = photos.isEmpty() ? null : photos.get(0);

        SalePropertyDocument.GeoPoint location = null;
        if (p.get("lat") != null && p.get("lng") != null) {
            location = new SalePropertyDocument.GeoPoint(
                    toDouble(p.get("lat")),
                    toDouble(p.get("lng"))
            );
        }

        return SalePropertyDocument.builder()
                .id(str(p.get("id")))
                .title(str(p.get("title")))
                .description(str(p.get("description")))
                .salePropertyType(str(p.get("salePropertyType")))
                .transactionType(str(p.get("transactionType")))
                .sellerType(str(p.get("sellerType")))
                .locality(str(p.get("locality")))
                .city(str(p.get("city")))
                .state(str(p.get("state")))
                .address(address)
                .pincode(str(p.get("pincode")))
                .location(location)
                .askingPricePaise(toLong(p.get("askingPricePaise")))
                .pricePerSqftPaise(toLong(p.get("pricePerSqftPaise")))
                .priceNegotiable(toBool(p.get("priceNegotiable")))
                .maintenancePaise(toLong(p.get("maintenancePaise")))
                .carpetAreaSqft(toInt(p.get("carpetAreaSqft")))
                .builtUpAreaSqft(toInt(p.get("builtUpAreaSqft")))
                .superBuiltUpAreaSqft(toInt(p.get("superBuiltUpAreaSqft")))
                .plotAreaSqft(toInt(p.get("plotAreaSqft")))
                .bedrooms(toInt(p.get("bedrooms")))
                .bathrooms(toInt(p.get("bathrooms")))
                .balconies(toInt(p.get("balconies")))
                .floorNumber(toInt(p.get("floorNumber")))
                .totalFloors(toInt(p.get("totalFloors")))
                .facing(str(p.get("facing")))
                .propertyAgeYears(toInt(p.get("propertyAgeYears")))
                .furnishing(str(p.get("furnishing")))
                .parkingCovered(toInt(p.get("parkingCovered")))
                .parkingOpen(toInt(p.get("parkingOpen")))
                .possessionStatus(str(p.get("possessionStatus")))
                .builderName(str(p.get("builderName")))
                .projectName(str(p.get("projectName")))
                .reraId(str(p.get("reraId")))
                .reraVerified(toBool(p.get("reraVerified")))
                .amenities(p.get("amenities") != null ? (List<String>) p.get("amenities") : null)
                .waterSupply(str(p.get("waterSupply")))
                .powerBackup(str(p.get("powerBackup")))
                .gatedCommunity(toBool(p.get("gatedCommunity")))
                .cornerProperty(toBool(p.get("cornerProperty")))
                .vastuCompliant(toBool(p.get("vastuCompliant")))
                .petAllowed(toBool(p.get("petAllowed")))
                .primaryPhotoUrl(primaryPhoto)
                .verified(toBool(p.get("verified")))
                .featured(toBool(p.get("featured")))
                .viewsCount(toInt(p.get("viewsCount")))
                .indexedAt(System.currentTimeMillis())
                .build();
    }

    private String str(Object o) { return o != null ? o.toString() : null; }
    private Long toLong(Object o) { return o instanceof Number n ? n.longValue() : null; }
    private Integer toInt(Object o) { return o instanceof Number n ? n.intValue() : null; }
    private Double toDouble(Object o) { return o instanceof Number n ? n.doubleValue() : null; }
    private Boolean toBool(Object o) { return o instanceof Boolean b ? b : null; }
}
