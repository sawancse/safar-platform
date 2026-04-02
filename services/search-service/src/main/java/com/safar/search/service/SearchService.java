package com.safar.search.service;

import com.safar.search.document.ListingDocument;
import com.safar.search.dto.FilterAggregations;
import com.safar.search.dto.SearchHitsResponse;
import com.safar.search.dto.SearchRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregation;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.RangeBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final ElasticsearchOperations esOps;

    public SearchHitsResponse search(SearchRequest req, UUID userId) {
        BoolQuery.Builder bool = buildFilters(req);

        var boolQuery = bool.build();

        var nqBuilder = NativeQuery.builder()
                .withPageable(PageRequest.of(req.page(), req.size()));

        // Sort
        String sortParam = req.sort();
        if ("price_asc".equals(sortParam)) {
            nqBuilder.withQuery(boolQuery._toQuery());
            nqBuilder.withSort(s -> s.field(f -> f.field("basePricePaise").order(SortOrder.Asc)));
        } else if ("price_desc".equals(sortParam)) {
            nqBuilder.withQuery(boolQuery._toQuery());
            nqBuilder.withSort(s -> s.field(f -> f.field("basePricePaise").order(SortOrder.Desc)));
        } else if ("rating_desc".equals(sortParam)) {
            nqBuilder.withQuery(boolQuery._toQuery());
            nqBuilder.withSort(s -> s.field(f -> f.field("avgRating").order(SortOrder.Desc)));
        } else if ("newest".equals(sortParam)) {
            nqBuilder.withQuery(boolQuery._toQuery());
            nqBuilder.withSort(s -> s.field(f -> f.field("indexedAt").order(SortOrder.Desc)));
        } else if ("distance_asc".equals(sortParam) && req.lat() != null && req.lng() != null) {
            nqBuilder.withQuery(boolQuery._toQuery());
            nqBuilder.withSort(s -> s.geoDistance(g -> g
                    .field("location")
                    .location(l -> l.latlon(ll -> ll.lat(req.lat()).lon(req.lng())))
                    .order(SortOrder.Asc)
                    .unit(co.elastic.clients.elasticsearch._types.DistanceUnit.Kilometers)));
        } else {
            // Default relevance sort: apply visibility boost via should clauses
            BoolQuery.Builder boostedBool = new BoolQuery.Builder();
            boostedBool.must(boolQuery._toQuery());
            // Boost listings with 3% visibility boost
            boostedBool.should(q -> q.term(t -> t.field("visibilityBoostPercent").value(3).boost(1.5f)));
            // Boost listings with 5% visibility boost
            boostedBool.should(q -> q.term(t -> t.field("visibilityBoostPercent").value(5).boost(3.0f)));
            // Boost preferred partner listings
            boostedBool.should(q -> q.term(t -> t.field("preferredPartner").value(true).boost(2.0f)));
            nqBuilder.withQuery(boostedBool.build()._toQuery());
            nqBuilder.withSort(s -> s.score(sc -> sc.order(SortOrder.Desc)));
        }

        // Add aggregations for filter counts
        addAggregations(nqBuilder);

        NativeQuery query = nqBuilder.build();
        SearchHits<ListingDocument> hits = esOps.search(query, ListingDocument.class);

        List<ListingDocument> listings = hits.getSearchHits().stream()
                .map(h -> h.getContent())
                .collect(Collectors.toList());

        FilterAggregations aggs = extractAggregations(hits);

        return new SearchHitsResponse(listings, hits.getTotalHits(), req.page(), req.size(), aggs);
    }

    private BoolQuery.Builder buildFilters(SearchRequest req) {
        BoolQuery.Builder bool = new BoolQuery.Builder();

        // ── Smart query parsing: extract type & city from natural language ──
        // "Home in Hyderabad" → type=HOME, city=Hyderabad
        // "PG near Gachibowli" → type=PG, address=Gachibowli
        // "villa goa" → type=VILLA, city=Goa
        String textQuery = req.query();
        String cityFilter = req.city();
        String localityFilter = null;
        List<String> typeFilter = req.type() != null ? new java.util.ArrayList<>(req.type()) : new java.util.ArrayList<>();

        if (StringUtils.hasText(textQuery)) {
            SmartQueryParser.ParsedQuery parsed = SmartQueryParser.parse(textQuery);
            log.debug("Smart parse '{}' → type={}, city={}, locality={}, nearMe={}, remaining='{}'",
                    textQuery, parsed.extractedType(), parsed.extractedCity(),
                    parsed.extractedLocality(), parsed.nearMe(), parsed.cleanedQuery());

            if (parsed.extractedType() != null && typeFilter.isEmpty()) {
                typeFilter.add(parsed.extractedType());
            }
            if (parsed.extractedCity() != null && !StringUtils.hasText(cityFilter)) {
                cityFilter = parsed.extractedCity();
            }
            if (parsed.extractedLocality() != null) {
                localityFilter = parsed.extractedLocality();
            }
            textQuery = parsed.cleanedQuery();
        }

        // Text search — use both parsed remainder AND original query for best recall
        if (StringUtils.hasText(textQuery)) {
            String finalTextQuery = textQuery;
            String origQuery = req.query();
            bool.must(q -> q.bool(tb -> {
                // Match parsed remainder (e.g., "Black berry" after extracting "hotel")
                tb.should(s -> s.multiMatch(m -> m
                        .query(finalTextQuery)
                        .fields("title^3", "description", "city^2", "address^2", "amenities")
                        .fuzziness("AUTO")
                        .operator(co.elastic.clients.elasticsearch._types.query_dsl.Operator.Or)));
                // Also match original full query against title (e.g., "Black berry hotel" matches "Blackberry Hotel")
                if (origQuery != null && !origQuery.equals(finalTextQuery)) {
                    tb.should(s -> s.multiMatch(m -> m
                            .query(origQuery)
                            .fields("title^3", "description", "address")
                            .fuzziness("AUTO")
                            .operator(co.elastic.clients.elasticsearch._types.query_dsl.Operator.Or)));
                }
                return tb.minimumShouldMatch("1");
            }));
        } else if (typeFilter.isEmpty() && !StringUtils.hasText(cityFilter) && !StringUtils.hasText(localityFilter)
                && StringUtils.hasText(req.query())) {
            // Nothing was extracted — use original query as full-text search against address+city+title
            String origQuery = req.query();
            bool.must(q -> q.multiMatch(m -> m
                    .query(origQuery)
                    .fields("title^3", "description", "city^2", "address^2", "amenities")
                    .fuzziness("AUTO")
                    .operator(co.elastic.clients.elasticsearch._types.query_dsl.Operator.Or)));
        } else if (!typeFilter.isEmpty() || StringUtils.hasText(cityFilter) || StringUtils.hasText(localityFilter)) {
            // Type/city/locality query — match all, let filters do the work
            bool.must(q -> q.matchAll(m -> m));
        }

        // City filter
        if (StringUtils.hasText(cityFilter)) {
            String cq = cityFilter;
            bool.filter(q -> q.bool(b -> b
                    .should(s -> s.match(m -> m.field("city").query(cq)))
                    .should(s -> s.match(m -> m.field("address").query(cq)))
                    .minimumShouldMatch("1")));
        }

        // Locality/area filter (e.g., "Gachibowli", "Madhapur" — search in address + city + title)
        if (StringUtils.hasText(localityFilter)) {
            String lf = localityFilter;
            bool.filter(q -> q.bool(b -> b
                    .should(s -> s.match(m -> m.field("address").query(lf).fuzziness("AUTO")))
                    .should(s -> s.match(m -> m.field("city").query(lf).fuzziness("AUTO")))
                    .should(s -> s.match(m -> m.field("title").query(lf).fuzziness("AUTO")))
                    .minimumShouldMatch("1")));
        }

        // Type filter
        if (!typeFilter.isEmpty()) {
            List<FieldValue> typeValues = typeFilter.stream()
                    .map(FieldValue::of).collect(Collectors.toList());
            bool.filter(q -> q.terms(t -> t.field("type").terms(v -> v.value(typeValues))));
        }

        if (req.priceMin() != null || req.priceMax() != null) {
            final Double minVal = req.priceMin() != null ? req.priceMin().doubleValue() : null;
            final Double maxVal = req.priceMax() != null ? req.priceMax().doubleValue() : null;
            bool.filter(q -> q.range(r -> r.number(n -> {
                n.field("basePricePaise");
                if (minVal != null) n.gte(minVal);
                if (maxVal != null) n.lte(maxVal);
                return n;
            })));
        }

        if (req.lat() != null && req.lng() != null) {
            String radius = (req.radiusKm() != null ? req.radiusKm() : 10.0) + "km";
            double lat = req.lat();
            double lng = req.lng();
            bool.filter(q -> q.geoDistance(g -> g
                    .field("location")
                    .location(l -> l.latlon(ll -> ll.lat(lat).lon(lng)))
                    .distance(radius)));
        }

        if (Boolean.TRUE.equals(req.instantBook())) {
            bool.filter(q -> q.term(t -> t.field("instantBook").value(true)));
        }

        if (req.minRating() != null) {
            final double minRat = req.minRating();
            bool.filter(q -> q.range(r -> r.number(n -> {
                n.field("avgRating");
                n.gte(minRat);
                return n;
            })));
        }

        if (Boolean.TRUE.equals(req.petFriendly())) {
            bool.filter(q -> q.term(t -> t.field("petFriendly").value(true)));
        }

        if (req.minBedrooms() != null && req.minBedrooms() > 0) {
            final double minBed = req.minBedrooms().doubleValue();
            bool.filter(q -> q.range(r -> r.number(n -> {
                n.field("bedrooms");
                n.gte(minBed);
                return n;
            })));
        }

        if (req.minBathrooms() != null && req.minBathrooms() > 0) {
            final double minBath = req.minBathrooms().doubleValue();
            bool.filter(q -> q.range(r -> r.number(n -> {
                n.field("bathrooms");
                n.gte(minBath);
                return n;
            })));
        }

        if (req.amenities() != null && !req.amenities().isEmpty()) {
            for (String amenity : req.amenities()) {
                bool.filter(q -> q.term(t -> t.field("amenities").value(amenity)));
            }
        }

        if (req.starRating() != null && req.starRating() > 0) {
            final double starVal = req.starRating().doubleValue();
            bool.filter(q -> q.range(r -> r.number(n -> {
                n.field("starRating");
                n.gte(starVal);
                return n;
            })));
        }

        if (Boolean.TRUE.equals(req.freeCancellation())) {
            bool.filter(q -> q.term(t -> t.field("freeCancellation").value(true)));
        }

        if (Boolean.TRUE.equals(req.noPrepayment())) {
            bool.filter(q -> q.term(t -> t.field("noPrepayment").value(true)));
        }

        if (StringUtils.hasText(req.cancellationPolicy())) {
            bool.filter(q -> q.term(t -> t.field("cancellationPolicy").value(req.cancellationPolicy())));
        }

        if (StringUtils.hasText(req.mealPlan())) {
            bool.filter(q -> q.term(t -> t.field("mealPlan").value(req.mealPlan())));
        }

        if (req.bedTypes() != null && !req.bedTypes().isEmpty()) {
            for (String bedType : req.bedTypes()) {
                bool.filter(q -> q.term(t -> t.field("bedTypes").value(bedType)));
            }
        }

        if (req.accessibilityFeatures() != null && !req.accessibilityFeatures().isEmpty()) {
            for (String feature : req.accessibilityFeatures()) {
                bool.filter(q -> q.term(t -> t.field("accessibilityFeatures").value(feature)));
            }
        }

        if (Boolean.TRUE.equals(req.aashrayReady())) {
            bool.filter(q -> q.term(t -> t.field("aashrayReady").value(true)));
        }

        if (Boolean.TRUE.equals(req.medicalStay())) {
            bool.filter(q -> q.term(t -> t.field("medicalStay").value(true)));
        }

        if (StringUtils.hasText(req.specialty())) {
            bool.filter(q -> q.term(t -> t.field("medicalSpecialties").value(req.specialty())));
        }

        if (StringUtils.hasText(req.procedure())) {
            bool.filter(q -> q.term(t -> t.field("procedureNames").value(req.procedure())));
        }

        // PG/Hotel filters
        if (StringUtils.hasText(req.occupancyType())) {
            bool.filter(q -> q.term(t -> t.field("occupancyType").value(req.occupancyType())));
        }

        if (StringUtils.hasText(req.foodType())) {
            bool.filter(q -> q.term(t -> t.field("foodType").value(req.foodType())));
        }

        if (Boolean.TRUE.equals(req.frontDesk24h())) {
            bool.filter(q -> q.term(t -> t.field("frontDesk24h").value(true)));
        }

        // Always filter verified only
        bool.filter(q -> q.term(t -> t.field("isVerified").value(true)));

        return bool;
    }

    private void addAggregations(NativeQueryBuilder nqBuilder) {
        nqBuilder.withAggregation("types",
                Aggregation.of(a -> a.terms(t -> t.field("type").size(20))));
        nqBuilder.withAggregation("amenities",
                Aggregation.of(a -> a.terms(t -> t.field("amenities").size(50))));
        nqBuilder.withAggregation("starRatings",
                Aggregation.of(a -> a.terms(t -> t.field("starRating").size(5))));
        nqBuilder.withAggregation("mealPlans",
                Aggregation.of(a -> a.terms(t -> t.field("mealPlan").size(10))));
        nqBuilder.withAggregation("cancellationPolicies",
                Aggregation.of(a -> a.terms(t -> t.field("cancellationPolicy").size(5))));
        nqBuilder.withAggregation("bedTypes",
                Aggregation.of(a -> a.terms(t -> t.field("bedTypes").size(10))));
        nqBuilder.withAggregation("accessibilityFeatures",
                Aggregation.of(a -> a.terms(t -> t.field("accessibilityFeatures").size(30))));

        nqBuilder.withAggregation("petFriendly",
                Aggregation.of(a -> a.filter(f -> f.term(t -> t.field("petFriendly").value(true)))));
        nqBuilder.withAggregation("instantBook",
                Aggregation.of(a -> a.filter(f -> f.term(t -> t.field("instantBook").value(true)))));
        nqBuilder.withAggregation("freeCancellation",
                Aggregation.of(a -> a.filter(f -> f.term(t -> t.field("freeCancellation").value(true)))));
        nqBuilder.withAggregation("noPrepayment",
                Aggregation.of(a -> a.filter(f -> f.term(t -> t.field("noPrepayment").value(true)))));

        var priceRangeList = List.of(
                co.elastic.clients.elasticsearch._types.aggregations.AggregationRange.of(rb -> rb.key("0-1000").to(100000.0)),
                co.elastic.clients.elasticsearch._types.aggregations.AggregationRange.of(rb -> rb.key("1000-3000").from(100000.0).to(300000.0)),
                co.elastic.clients.elasticsearch._types.aggregations.AggregationRange.of(rb -> rb.key("3000-5000").from(300000.0).to(500000.0)),
                co.elastic.clients.elasticsearch._types.aggregations.AggregationRange.of(rb -> rb.key("5000-10000").from(500000.0).to(1000000.0)),
                co.elastic.clients.elasticsearch._types.aggregations.AggregationRange.of(rb -> rb.key("10000-20000").from(1000000.0).to(2000000.0)),
                co.elastic.clients.elasticsearch._types.aggregations.AggregationRange.of(rb -> rb.key("20000+").from(2000000.0))
        );
        nqBuilder.withAggregation("priceRanges",
                Aggregation.of(a -> a.range(r -> r.field("basePricePaise").ranges(priceRangeList))));

        var ratingRangeList = List.of(
                co.elastic.clients.elasticsearch._types.aggregations.AggregationRange.of(rb -> rb.key("9+").from(9.0)),
                co.elastic.clients.elasticsearch._types.aggregations.AggregationRange.of(rb -> rb.key("8+").from(8.0)),
                co.elastic.clients.elasticsearch._types.aggregations.AggregationRange.of(rb -> rb.key("7+").from(7.0)),
                co.elastic.clients.elasticsearch._types.aggregations.AggregationRange.of(rb -> rb.key("6+").from(6.0))
        );
        nqBuilder.withAggregation("ratingRanges",
                Aggregation.of(a -> a.range(r -> r.field("avgRating").ranges(ratingRangeList))));

        nqBuilder.withAggregation("bedroomCounts",
                Aggregation.of(a -> a.terms(t -> t.field("bedrooms").size(10))));
        nqBuilder.withAggregation("bathroomCounts",
                Aggregation.of(a -> a.terms(t -> t.field("bathrooms").size(10))));

        nqBuilder.withAggregation("medicalStay",
                Aggregation.of(a -> a.filter(f -> f.term(t -> t.field("medicalStay").value(true)))));
        nqBuilder.withAggregation("medicalSpecialties",
                Aggregation.of(a -> a.terms(t -> t.field("medicalSpecialties").size(30))));

        // PG/Hotel aggregations
        nqBuilder.withAggregation("occupancyTypes",
                Aggregation.of(a -> a.terms(t -> t.field("occupancyType").size(5))));
        nqBuilder.withAggregation("foodTypes",
                Aggregation.of(a -> a.terms(t -> t.field("foodType").size(5))));
        nqBuilder.withAggregation("frontDesk24h",
                Aggregation.of(a -> a.filter(f -> f.term(t -> t.field("frontDesk24h").value(true)))));
    }

    private FilterAggregations extractAggregations(SearchHits<ListingDocument> hits) {
        try {
            if (hits.getAggregations() == null) return null;

            ElasticsearchAggregations esAggs = (ElasticsearchAggregations) hits.getAggregations();

            // Build name -> Aggregate map from the list
            Map<String, Aggregate> aggMap = new HashMap<>();
            for (ElasticsearchAggregation ea : esAggs.aggregations()) {
                aggMap.put(ea.aggregation().getName(), ea.aggregation().getAggregate());
            }

            return new FilterAggregations(
                    extractTerms(aggMap.get("types")),
                    extractTerms(aggMap.get("amenities")),
                    extractTerms(aggMap.get("starRatings")),
                    extractTerms(aggMap.get("mealPlans")),
                    extractTerms(aggMap.get("cancellationPolicies")),
                    extractTerms(aggMap.get("bedTypes")),
                    extractTerms(aggMap.get("accessibilityFeatures")),
                    extractFilterDocCount(aggMap.get("petFriendly")),
                    extractFilterDocCount(aggMap.get("instantBook")),
                    extractFilterDocCount(aggMap.get("freeCancellation")),
                    extractFilterDocCount(aggMap.get("noPrepayment")),
                    extractRanges(aggMap.get("priceRanges")),
                    extractRanges(aggMap.get("ratingRanges")),
                    extractTerms(aggMap.get("bedroomCounts")),
                    extractTerms(aggMap.get("bathroomCounts")),
                    extractFilterDocCount(aggMap.get("medicalStay")),
                    extractTerms(aggMap.get("medicalSpecialties")),
                    // PG/Hotel aggregations
                    extractTerms(aggMap.get("occupancyTypes")),
                    extractTerms(aggMap.get("foodTypes")),
                    extractFilterDocCount(aggMap.get("frontDesk24h"))
            );
        } catch (Exception e) {
            log.warn("Could not extract aggregations: {}", e.getMessage());
            return null;
        }
    }

    private Map<String, Long> extractTerms(Aggregate agg) {
        Map<String, Long> result = new LinkedHashMap<>();
        if (agg == null) return result;
        try {
            if (agg.isSterms()) {
                for (StringTermsBucket b : agg.sterms().buckets().array()) {
                    result.put(b.key().stringValue(), b.docCount());
                }
            } else if (agg.isLterms()) {
                for (LongTermsBucket b : agg.lterms().buckets().array()) {
                    result.put(String.valueOf(b.key()), b.docCount());
                }
            }
        } catch (Exception e) {
            log.debug("Terms extraction error: {}", e.getMessage());
        }
        return result;
    }

    private Map<String, Long> extractRanges(Aggregate agg) {
        Map<String, Long> result = new LinkedHashMap<>();
        if (agg == null) return result;
        try {
            for (RangeBucket b : agg.range().buckets().array()) {
                if (b.docCount() > 0) {
                    result.put(b.key(), b.docCount());
                }
            }
        } catch (Exception e) {
            log.debug("Range extraction error: {}", e.getMessage());
        }
        return result;
    }

    private long extractFilterDocCount(Aggregate agg) {
        if (agg == null) return 0;
        try {
            return agg.filter().docCount();
        } catch (Exception e) {
            return 0;
        }
    }

    public List<String> autocomplete(String q) {
        // Smart parse first — generate composite suggestions
        SmartQueryParser.ParsedQuery parsed = SmartQueryParser.parse(q);
        List<String> smartSuggestions = new java.util.ArrayList<>();

        // If we extracted type + city, suggest the composed query
        if (parsed.extractedType() != null && parsed.extractedCity() != null) {
            String typeName = parsed.extractedType().substring(0, 1) + parsed.extractedType().substring(1).toLowerCase().replace('_', ' ');
            smartSuggestions.add(typeName + " in " + parsed.extractedCity());
        }

        // Search ES for matching cities, titles, addresses
        String searchTerm = parsed.extractedCity() != null ? parsed.extractedCity()
                : (parsed.cleanedQuery() != null && !parsed.cleanedQuery().isBlank() ? parsed.cleanedQuery() : q);
        BoolQuery.Builder bool = new BoolQuery.Builder();
        String finalSearchTerm = searchTerm;

        // Type-only query (e.g., "pg", "hotel", "coliving") — match all of that type
        boolean typeOnlyQuery = parsed.extractedType() != null
                && parsed.extractedCity() == null
                && (parsed.cleanedQuery() == null || parsed.cleanedQuery().isBlank());

        if (typeOnlyQuery) {
            bool.must(mq -> mq.matchAll(m -> m));
        } else {
            bool.must(mq -> mq.bool(b -> b
                    .should(s -> s.matchPhrasePrefix(m -> m.field("city").query(finalSearchTerm).boost(3.0f)))
                    .should(s -> s.matchPhrasePrefix(m -> m.field("title").query(q).boost(2.0f)))
                    .should(s -> s.matchPhrasePrefix(m -> m.field("address").query(finalSearchTerm)))
                    .minimumShouldMatch("1")));
        }
        bool.filter(fq -> fq.term(t -> t.field("isVerified").value(true)));

        // If type was extracted, filter by type
        if (parsed.extractedType() != null) {
            String extractedType = parsed.extractedType();
            bool.filter(fq -> fq.term(t -> t.field("type").value(extractedType)));
        }

        NativeQuery query = NativeQuery.builder()
                .withQuery(bool.build()._toQuery())
                .withPageable(PageRequest.of(0, 10))
                .build();

        SearchHits<ListingDocument> hits = esOps.search(query, ListingDocument.class);

        java.util.Set<String> seen = new java.util.LinkedHashSet<>(smartSuggestions);
        for (var hit : hits.getSearchHits()) {
            ListingDocument doc = hit.getContent();
            if (doc.getCity() != null) seen.add(doc.getCity());
            if (doc.getTitle() != null && doc.getTitle().toLowerCase().contains(q.toLowerCase())) {
                seen.add(doc.getTitle());
            }
        }
        return seen.stream().limit(8).collect(Collectors.toList());
    }

    public List<ListingDocument> getTrending(String city) {
        BoolQuery.Builder bool = new BoolQuery.Builder();
        bool.filter(q -> q.match(m -> m.field("city").query(city)));
        bool.filter(q -> q.term(t -> t.field("isVerified").value(true)));

        NativeQuery query = NativeQuery.builder()
                .withQuery(bool.build()._toQuery())
                .withPageable(PageRequest.of(0, 10))
                .withSort(s -> s.field(f -> f.field("avgRating").order(SortOrder.Desc)))
                .build();

        return esOps.search(query, ListingDocument.class).getSearchHits().stream()
                .map(h -> h.getContent())
                .collect(Collectors.toList());
    }

    public List<ListingDocument> getNearby(Double lat, Double lng, Double radiusKm, Integer limit, String type) {
        double radius = radiusKm != null ? radiusKm : getSmartRadius(type);
        int maxResults = limit != null ? limit : 20;

        BoolQuery.Builder bool = new BoolQuery.Builder();
        bool.filter(q -> q.geoDistance(g -> g
                .field("location")
                .location(l -> l.latlon(ll -> ll.lat(lat).lon(lng)))
                .distance(radius + "km")));
        bool.filter(q -> q.term(t -> t.field("isVerified").value(true)));

        if (type != null && !type.isBlank()) {
            bool.filter(q -> q.term(t -> t.field("type").value(type)));
        }

        NativeQuery query = NativeQuery.builder()
                .withQuery(bool.build()._toQuery())
                .withPageable(PageRequest.of(0, maxResults))
                .withSort(s -> s.geoDistance(g -> g
                        .field("location")
                        .location(l -> l.latlon(ll -> ll.lat(lat).lon(lng)))
                        .order(SortOrder.Asc)
                        .unit(co.elastic.clients.elasticsearch._types.DistanceUnit.Kilometers)))
                .build();

        return esOps.search(query, ListingDocument.class).getSearchHits().stream()
                .map(h -> h.getContent())
                .collect(Collectors.toList());
    }

    private double getSmartRadius(String type) {
        if (type == null) return 10.0;
        return switch (type.toUpperCase()) {
            case "HOTEL", "BUDGET_HOTEL" -> 3.0;
            case "PG", "COLIVING" -> 5.0;
            case "HOMESTAY", "VILLA", "FARMSTAY" -> 10.0;
            default -> 10.0;
        };
    }

    public void indexListing(ListingDocument doc) {
        esOps.save(doc);
        log.info("Indexed listing {}", doc.getId());
    }

    public void deleteListing(String id) {
        try {
            esOps.delete(id, ListingDocument.class);
            log.info("Deleted listing {} from ES", id);
        } catch (Exception e) {
            log.warn("Failed to delete listing {} from ES: {}", id, e.getMessage());
        }
    }

    public ListingDocument getById(String id) {
        return esOps.get(id, ListingDocument.class);
    }

    public org.springframework.data.elasticsearch.core.IndexOperations getIndexOps() {
        return esOps.indexOps(ListingDocument.class);
    }
}
