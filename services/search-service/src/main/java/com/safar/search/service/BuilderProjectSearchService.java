package com.safar.search.service;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.safar.search.document.BuilderProjectDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class BuilderProjectSearchService {

    private final ElasticsearchOperations esOps;

    public Map<String, Object> search(String query, String city, String locality,
                                       String projectStatus, Long priceMin, Long priceMax,
                                       List<Integer> bhk, Boolean reraVerified,
                                       Double lat, Double lng, Double radiusKm,
                                       String sort, int page, int size) {

        NativeQueryBuilder qb = NativeQuery.builder()
                .withPageable(PageRequest.of(page, size));

        BoolQuery.Builder bool = new BoolQuery.Builder();

        if (query != null && !query.isBlank()) {
            bool.must(Query.of(q -> q.multiMatch(m -> m
                    .query(query)
                    .fields("projectName^3", "builderName^2", "description", "city^2", "locality^2", "address")
                    .fuzziness("AUTO")
            )));
        }

        if (city != null) {
            bool.filter(Query.of(q -> q.match(m -> m.field("city").query(city))));
        }
        if (locality != null) {
            bool.filter(Query.of(q -> q.match(m -> m.field("locality").query(locality))));
        }
        if (projectStatus != null) {
            bool.filter(Query.of(q -> q.term(t -> t.field("projectStatus").value(projectStatus))));
        }
        if (priceMin != null || priceMax != null) {
            bool.filter(Query.of(q -> q.range(r -> r.number(n -> {
                n.field("minPricePaise");
                if (priceMin != null) n.gte(priceMin.doubleValue());
                if (priceMax != null) n.lte(priceMax.doubleValue());
                return n;
            }))));
        }
        if (bhk != null && !bhk.isEmpty()) {
            for (Integer b : bhk) {
                bool.should(Query.of(q -> q.range(r -> r.number(n -> {
                    n.field("minBhk"); n.lte(b.doubleValue()); return n;
                }))));
                bool.should(Query.of(q -> q.range(r -> r.number(n -> {
                    n.field("maxBhk"); n.gte(b.doubleValue()); return n;
                }))));
            }
            bool.minimumShouldMatch("1");
        }
        if (Boolean.TRUE.equals(reraVerified)) {
            bool.filter(Query.of(q -> q.term(t -> t.field("reraVerified").value(true))));
        }
        if (lat != null && lng != null) {
            double radius = radiusKm != null ? radiusKm : 15.0;
            bool.filter(Query.of(q -> q.geoDistance(g -> g
                    .field("location")
                    .location(l -> l.latlon(ll -> ll.lat(lat).lon(lng)))
                    .distance(radius + "km")
            )));
        }

        qb.withQuery(Query.of(q -> q.bool(bool.build())));

        String s = sort != null ? sort : "relevance";
        switch (s) {
            case "price_asc" -> qb.withSort(sr -> sr.field(f -> f.field("minPricePaise").order(SortOrder.Asc)));
            case "price_desc" -> qb.withSort(sr -> sr.field(f -> f.field("maxPricePaise").order(SortOrder.Desc)));
            case "newest" -> qb.withSort(sr -> sr.field(f -> f.field("indexedAt").order(SortOrder.Desc)));
            case "possession" -> qb.withSort(sr -> sr.field(f -> f.field("possessionDate").order(SortOrder.Asc)));
            default -> qb.withSort(sr -> sr.score(sc -> sc.order(SortOrder.Desc)));
        }

        SearchHits<BuilderProjectDocument> hits = esOps.search(qb.build(), BuilderProjectDocument.class);
        List<BuilderProjectDocument> content = hits.getSearchHits().stream()
                .map(SearchHit::getContent).toList();

        Map<String, Object> result = new HashMap<>();
        result.put("content", content);
        result.put("totalHits", hits.getTotalHits());
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    public List<Map<String, String>> autocomplete(String q) {
        if (q == null || q.isBlank()) return List.of();

        NativeQuery query = NativeQuery.builder()
                .withQuery(Query.of(qr -> qr.multiMatch(m -> m
                        .query(q)
                        .fields("projectName^3", "builderName^2", "city^2", "locality^2")
                        .fuzziness("AUTO")
                        .type(co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.BestFields)
                )))
                .withPageable(PageRequest.of(0, 8))
                .build();

        SearchHits<BuilderProjectDocument> hits = esOps.search(query, BuilderProjectDocument.class);
        return hits.getSearchHits().stream().map(h -> {
            BuilderProjectDocument d = h.getContent();
            return Map.of(
                    "id", d.getId() != null ? d.getId() : "",
                    "projectName", d.getProjectName() != null ? d.getProjectName() : "",
                    "builderName", d.getBuilderName() != null ? d.getBuilderName() : "",
                    "city", d.getCity() != null ? d.getCity() : "",
                    "locality", d.getLocality() != null ? d.getLocality() : ""
            );
        }).toList();
    }

    public void index(BuilderProjectDocument doc) {
        esOps.save(doc);
    }

    public void delete(String id) {
        esOps.delete(id, BuilderProjectDocument.class);
    }
}
