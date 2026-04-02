package com.safar.search.service;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.safar.search.document.ExperienceDocument;
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
public class ExperienceSearchService {

    private final ElasticsearchOperations esOps;

    public Map<String, Object> search(String query, String city, List<String> category,
                                       Long priceMin, Long priceMax,
                                       Double lat, Double lng, Double radiusKm,
                                       String sort, int page, int size) {

        NativeQueryBuilder qb = NativeQuery.builder()
                .withPageable(PageRequest.of(page, size));

        BoolQuery.Builder bool = new BoolQuery.Builder();

        // Text search
        if (query != null && !query.isBlank()) {
            bool.must(Query.of(q -> q.multiMatch(m -> m
                    .query(query)
                    .fields("title^3", "description", "city^2", "locationName^2")
                    .fuzziness("AUTO")
            )));
        }

        // City filter
        if (city != null && !city.isBlank()) {
            bool.filter(Query.of(q -> q.match(m -> m.field("city").query(city))));
        }

        // Category filter
        if (category != null && !category.isEmpty()) {
            bool.filter(Query.of(q -> q.terms(t -> t
                    .field("category")
                    .terms(tv -> tv.value(category.stream()
                            .map(FieldValue::of).toList()))
            )));
        }

        // Price range
        if (priceMin != null || priceMax != null) {
            bool.filter(Query.of(q -> q.range(r -> r.number(n -> {
                n.field("pricePaise");
                if (priceMin != null) n.gte(priceMin.doubleValue());
                if (priceMax != null) n.lte(priceMax.doubleValue());
                return n;
            }))));
        }

        // Geo-distance
        if (lat != null && lng != null) {
            double radius = radiusKm != null ? radiusKm : 20.0;
            bool.filter(Query.of(q -> q.geoDistance(g -> g
                    .field("location")
                    .location(l -> l.latlon(ll -> ll.lat(lat).lon(lng)))
                    .distance(radius + "km")
            )));
        }

        // Exclude private experiences from public search
        bool.filter(Query.of(q -> q.bool(b -> b
                .should(Query.of(s -> s.term(t -> t.field("isPrivate").value(false))))
                .should(Query.of(s -> s.bool(nb -> nb.mustNot(Query.of(mn -> mn.exists(e -> e.field("isPrivate")))))))
        )));

        qb.withQuery(Query.of(q -> q.bool(bool.build())));

        // Sorting
        String sortBy = sort != null ? sort : "relevance";
        switch (sortBy) {
            case "price_asc" -> qb.withSort(s -> s.field(f -> f.field("pricePaise").order(SortOrder.Asc)));
            case "price_desc" -> qb.withSort(s -> s.field(f -> f.field("pricePaise").order(SortOrder.Desc)));
            case "rating_desc" -> qb.withSort(s -> s.field(f -> f.field("rating").order(SortOrder.Desc)));
            case "newest" -> qb.withSort(s -> s.field(f -> f.field("indexedAt").order(SortOrder.Desc)));
            default -> qb.withSort(s -> s.score(sc -> sc.order(SortOrder.Desc)));
        }

        NativeQuery nq = qb.build();
        SearchHits<ExperienceDocument> hits = esOps.search(nq, ExperienceDocument.class);

        List<ExperienceDocument> content = hits.getSearchHits().stream()
                .map(SearchHit::getContent).toList();

        Map<String, Object> result = new HashMap<>();
        result.put("content", content);
        result.put("totalHits", hits.getTotalHits());
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    public List<ExperienceDocument> autocomplete(String q) {
        if (q == null || q.length() < 2) return List.of();

        NativeQuery query = NativeQuery.builder()
                .withQuery(Query.of(qr -> qr.multiMatch(m -> m
                        .query(q)
                        .fields("title^2", "city^3", "locationName^2")
                        .type(co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.PhrasePrefix)
                )))
                .withPageable(PageRequest.of(0, 8))
                .build();

        return esOps.search(query, ExperienceDocument.class)
                .getSearchHits().stream().map(SearchHit::getContent).toList();
    }

    public List<ExperienceDocument> trending(String city, int limit) {
        BoolQuery.Builder bool = new BoolQuery.Builder();
        if (city != null && !city.isBlank()) {
            bool.filter(Query.of(q -> q.match(m -> m.field("city").query(city))));
        }

        NativeQuery query = NativeQuery.builder()
                .withQuery(Query.of(q -> q.bool(bool.build())))
                .withSort(s -> s.field(f -> f.field("rating").order(SortOrder.Desc)))
                .withPageable(PageRequest.of(0, limit))
                .build();

        return esOps.search(query, ExperienceDocument.class)
                .getSearchHits().stream().map(SearchHit::getContent).toList();
    }

    public void index(ExperienceDocument doc) {
        esOps.save(doc);
        log.debug("Indexed experience: {}", doc.getId());
    }

    public void delete(String id) {
        esOps.delete(id, ExperienceDocument.class);
        log.debug("Deleted experience from index: {}", id);
    }
}
