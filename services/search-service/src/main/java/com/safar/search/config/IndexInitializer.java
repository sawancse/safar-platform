package com.safar.search.config;

import com.safar.search.document.ListingDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class IndexInitializer {

    private final ElasticsearchOperations elasticsearchOperations;

    @Value("${elasticsearch.index.shards:1}")
    private int shards;

    @Value("${elasticsearch.index.replicas:0}")
    private int replicas;

    @Value("${elasticsearch.index.force-recreate:false}")
    private boolean forceRecreate;

    @EventListener(ApplicationReadyEvent.class)
    public void initIndex() {
        try {
            IndexOperations indexOps = elasticsearchOperations.indexOps(ListingDocument.class);

            if (indexOps.exists() && forceRecreate) {
                indexOps.delete();
                log.info("Deleted existing 'listings' index (force-recreate=true)");
            }

            // Check if existing index has correct geo_point mapping for location
            if (indexOps.exists()) {
                boolean needsRecreate = false;
                try {
                    Map<String, Object> mapping = indexOps.getMapping();
                    // Check if location field is geo_point
                    Map<String, Object> properties = getNestedMap(mapping, "properties");
                    if (properties != null) {
                        Map<String, Object> locationMapping = getNestedMap(properties, "location");
                        if (locationMapping == null || !"geo_point".equals(locationMapping.get("type"))) {
                            log.warn("Index 'listings' has wrong mapping for 'location' field (not geo_point) — recreating");
                            needsRecreate = true;
                        }
                    }
                } catch (Exception e) {
                    log.debug("Could not verify mapping: {}", e.getMessage());
                }

                if (needsRecreate) {
                    indexOps.delete();
                    log.info("Deleted index with bad mapping");
                }
            }

            if (!indexOps.exists()) {
                Document settings = Document.from(Map.of(
                        "index.number_of_shards", shards,
                        "index.number_of_replicas", replicas
                ));
                indexOps.create(settings);
                indexOps.putMapping(indexOps.createMapping(ListingDocument.class));
                log.info("Created 'listings' index with shards={}, replicas={}, geo_point mapping for location field", shards, replicas);
            } else {
                // Update mapping in case new fields were added (won't change existing field types)
                try {
                    indexOps.putMapping(indexOps.createMapping(ListingDocument.class));
                } catch (Exception e) {
                    log.debug("Mapping update skipped: {}", e.getMessage());
                }
                log.info("Index 'listings' already exists, mapping verified");
            }
        } catch (Exception e) {
            log.warn("Index initialization skipped — Elasticsearch may be unavailable: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getNestedMap(Map<String, Object> map, String key) {
        if (map == null) return null;
        Object val = map.get(key);
        return val instanceof Map ? (Map<String, Object>) val : null;
    }
}
