package com.safar.search.service;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.safar.search.document.SalePropertyDocument;
import com.safar.search.dto.SalePropertySearchRequest;
import com.safar.search.dto.SaleSearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SalePropertySearchService {

    private final ElasticsearchOperations esOps;

    public SaleSearchResponse search(SalePropertySearchRequest req) {
        NativeQueryBuilder qb = NativeQuery.builder()
                .withPageable(PageRequest.of(req.page(), req.size()));

        BoolQuery.Builder bool = new BoolQuery.Builder();

        // Text search
        if (req.query() != null && !req.query().isBlank()) {
            bool.must(Query.of(q -> q.multiMatch(m -> m
                    .query(req.query())
                    .fields("title^3", "description", "city^2", "locality^2", "address^2", "projectName", "builderName")
                    .fuzziness("AUTO")
            )));
        }

        // City filter
        if (req.city() != null && !req.city().isBlank()) {
            bool.filter(Query.of(q -> q.bool(b -> b
                    .should(Query.of(s -> s.match(mm -> mm.field("city").query(req.city()))))
                    .should(Query.of(s -> s.match(mm -> mm.field("address").query(req.city()))))
            )));
        }

        // Locality filter
        if (req.locality() != null && !req.locality().isBlank()) {
            bool.filter(Query.of(q -> q.match(m -> m.field("locality").query(req.locality()))));
        }

        // Property type filter
        if (req.salePropertyType() != null && !req.salePropertyType().isEmpty()) {
            bool.filter(Query.of(q -> q.terms(t -> t
                    .field("salePropertyType")
                    .terms(tv -> tv.value(req.salePropertyType().stream()
                            .map(co.elastic.clients.elasticsearch._types.FieldValue::of).toList()))
            )));
        }

        // Transaction type
        if (req.transactionType() != null && !req.transactionType().isEmpty()) {
            bool.filter(Query.of(q -> q.terms(t -> t
                    .field("transactionType")
                    .terms(tv -> tv.value(req.transactionType().stream()
                            .map(co.elastic.clients.elasticsearch._types.FieldValue::of).toList()))
            )));
        }

        // Seller type
        if (req.sellerType() != null && !req.sellerType().isEmpty()) {
            bool.filter(Query.of(q -> q.terms(t -> t
                    .field("sellerType")
                    .terms(tv -> tv.value(req.sellerType().stream()
                            .map(co.elastic.clients.elasticsearch._types.FieldValue::of).toList()))
            )));
        }

        // Price range
        if (req.priceMin() != null || req.priceMax() != null) {
            bool.filter(Query.of(q -> q.range(r -> r.number(n -> {
                n.field("askingPricePaise");
                if (req.priceMin() != null) n.gte(req.priceMin().doubleValue());
                if (req.priceMax() != null) n.lte(req.priceMax().doubleValue());
                return n;
            }))));
        }

        // Bedrooms
        if (req.bedrooms() != null && !req.bedrooms().isEmpty()) {
            bool.filter(Query.of(q -> q.terms(t -> t
                    .field("bedrooms")
                    .terms(tv -> tv.value(req.bedrooms().stream()
                            .map(i -> co.elastic.clients.elasticsearch._types.FieldValue.of((long) i)).toList()))
            )));
        }

        // Area range
        if (req.minArea() != null || req.maxArea() != null) {
            bool.filter(Query.of(q -> q.range(r -> r.number(n -> {
                n.field("carpetAreaSqft");
                if (req.minArea() != null) n.gte(req.minArea().doubleValue());
                if (req.maxArea() != null) n.lte(req.maxArea().doubleValue());
                return n;
            }))));
        }

        // Possession status
        if (req.possessionStatus() != null) {
            bool.filter(Query.of(q -> q.term(t -> t.field("possessionStatus").value(req.possessionStatus()))));
        }

        // Furnishing
        if (req.furnishing() != null) {
            bool.filter(Query.of(q -> q.term(t -> t.field("furnishing").value(req.furnishing()))));
        }

        // Facing
        if (req.facing() != null && !req.facing().isEmpty()) {
            bool.filter(Query.of(q -> q.terms(t -> t
                    .field("facing")
                    .terms(tv -> tv.value(req.facing().stream()
                            .map(co.elastic.clients.elasticsearch._types.FieldValue::of).toList()))
            )));
        }

        // Floor range
        if (req.minFloor() != null || req.maxFloor() != null) {
            bool.filter(Query.of(q -> q.range(r -> r.number(n -> {
                n.field("floorNumber");
                if (req.minFloor() != null) n.gte(req.minFloor().doubleValue());
                if (req.maxFloor() != null) n.lte(req.maxFloor().doubleValue());
                return n;
            }))));
        }

        // Max age
        if (req.maxAge() != null) {
            bool.filter(Query.of(q -> q.range(r -> r.number(n -> {
                n.field("propertyAgeYears");
                n.lte(req.maxAge().doubleValue());
                return n;
            }))));
        }

        // Boolean filters
        if (Boolean.TRUE.equals(req.reraVerified())) {
            bool.filter(Query.of(q -> q.term(t -> t.field("reraVerified").value(true))));
        }
        if (Boolean.TRUE.equals(req.vastuCompliant())) {
            bool.filter(Query.of(q -> q.term(t -> t.field("vastuCompliant").value(true))));
        }
        if (Boolean.TRUE.equals(req.gatedCommunity())) {
            bool.filter(Query.of(q -> q.term(t -> t.field("gatedCommunity").value(true))));
        }
        if (Boolean.TRUE.equals(req.petAllowed())) {
            bool.filter(Query.of(q -> q.term(t -> t.field("petAllowed").value(true))));
        }
        if (Boolean.TRUE.equals(req.cornerProperty())) {
            bool.filter(Query.of(q -> q.term(t -> t.field("cornerProperty").value(true))));
        }
        if (Boolean.TRUE.equals(req.verified())) {
            bool.filter(Query.of(q -> q.term(t -> t.field("verified").value(true))));
        }

        // Amenities
        if (req.amenities() != null && !req.amenities().isEmpty()) {
            for (String amenity : req.amenities()) {
                bool.filter(Query.of(q -> q.term(t -> t.field("amenities").value(amenity))));
            }
        }

        // Geo-distance
        if (req.lat() != null && req.lng() != null) {
            double radius = req.radiusKm() != null ? req.radiusKm() : 10.0;
            bool.filter(Query.of(q -> q.geoDistance(g -> g
                    .field("location")
                    .location(l -> l.latlon(ll -> ll.lat(req.lat()).lon(req.lng())))
                    .distance(radius + "km")
            )));
        }

        // Always match all documents (so should clauses are optional boosters, not required)
        bool.must(Query.of(q -> q.matchAll(m -> m)));

        // Boost featured
        bool.should(Query.of(q -> q.term(t -> t.field("featured").value(true).boost(3.0f))));

        qb.withQuery(Query.of(q -> q.bool(bool.build())));

        // Sorting
        String sort = req.sort() != null ? req.sort() : "relevance";
        switch (sort) {
            case "price_asc" -> qb.withSort(s -> s.field(f -> f.field("askingPricePaise").order(SortOrder.Asc)));
            case "price_desc" -> qb.withSort(s -> s.field(f -> f.field("askingPricePaise").order(SortOrder.Desc)));
            case "newest" -> qb.withSort(s -> s.field(f -> f.field("indexedAt").order(SortOrder.Desc)));
            case "area_desc" -> qb.withSort(s -> s.field(f -> f.field("carpetAreaSqft").order(SortOrder.Desc)));
            case "price_per_sqft" -> qb.withSort(s -> s.field(f -> f.field("pricePerSqftPaise").order(SortOrder.Asc)));
            default -> qb.withSort(s -> s.score(sc -> sc.order(SortOrder.Desc)));
        }

        NativeQuery query = qb.build();
        SearchHits<SalePropertyDocument> hits = esOps.search(query, SalePropertyDocument.class);

        List<SalePropertyDocument> content = hits.getSearchHits().stream()
                .map(SearchHit::getContent).toList();

        // Build aggregations map
        Map<String, Map<String, Long>> aggregations = new HashMap<>();

        return new SaleSearchResponse(content, hits.getTotalHits(), req.page(), req.size(), aggregations);
    }

    public List<SalePropertyDocument> autocomplete(String q) {
        if (q == null || q.length() < 2) return List.of();

        NativeQuery query = NativeQuery.builder()
                .withQuery(Query.of(qr -> qr.multiMatch(m -> m
                        .query(q)
                        .fields("title^2", "locality^3", "city^3", "projectName^2", "builderName")
                        .type(co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.PhrasePrefix)
                )))
                .withPageable(PageRequest.of(0, 8))
                .build();

        return esOps.search(query, SalePropertyDocument.class)
                .getSearchHits().stream().map(SearchHit::getContent).toList();
    }

    public List<SalePropertyDocument> getRecentByCity(String city, int limit) {
        NativeQuery query = NativeQuery.builder()
                .withQuery(Query.of(q -> q.term(t -> t.field("city").value(city))))
                .withSort(s -> s.field(f -> f.field("indexedAt").order(SortOrder.Desc)))
                .withPageable(PageRequest.of(0, limit))
                .build();

        return esOps.search(query, SalePropertyDocument.class)
                .getSearchHits().stream().map(SearchHit::getContent).toList();
    }

    public void index(SalePropertyDocument doc) {
        esOps.save(doc);
        log.debug("Indexed sale property: {}", doc.getId());
    }

    public void delete(String id) {
        esOps.delete(id, SalePropertyDocument.class);
        log.debug("Deleted sale property from index: {}", id);
    }
}
