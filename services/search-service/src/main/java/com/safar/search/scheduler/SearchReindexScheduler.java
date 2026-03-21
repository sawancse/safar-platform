package com.safar.search.scheduler;

import com.safar.search.document.ListingDocument;
import com.safar.search.service.ListingServiceClient;
import com.safar.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Automatic ES reindexing:
 * 1. On startup — full reindex when search-service starts
 * 2. Every 15 minutes — safety-net sync for any missed Kafka events
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SearchReindexScheduler {

    private final SearchService searchService;
    private final ListingServiceClient listingClient;

    /**
     * Full reindex on startup (after all beans are ready).
     * Ensures ES is populated even after a restart.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        log.info("Search service started — running full reindex...");
        try {
            int count = reindexAll();
            log.info("Startup reindex complete: {} listings indexed", count);
        } catch (Exception e) {
            log.error("Startup reindex failed: {} — search may be stale until next scheduled run", e.getMessage());
        }
    }

    /**
     * Safety-net: reindex every 15 minutes to catch any missed Kafka events.
     */
    @Scheduled(fixedRate = 900000) // 15 minutes
    public void periodicReindex() {
        try {
            int count = reindexAll();
            log.debug("Periodic reindex: {} listings synced", count);
        } catch (Exception e) {
            log.warn("Periodic reindex failed: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private int reindexAll() {
        RestTemplate rt = new RestTemplate();
        int totalCount = 0;
        int pageNum = 0;

        while (true) {
            String url = "http://localhost:8083/api/v1/listings?size=100&page=" + pageNum;
            Map<String, Object> page;
            try {
                page = rt.getForObject(url, Map.class);
            } catch (Exception e) {
                log.warn("Cannot reach listing-service at page {}: {}", pageNum, e.getMessage());
                break;
            }
            if (page == null) break;

            List<Map<String, Object>> content = (List<Map<String, Object>>) page.get("content");
            if (content == null || content.isEmpty()) break;

            for (var listing : content) {
                String id = (String) listing.get("id");
                try {
                    ListingDocument doc = listingClient.getListingDocument(UUID.fromString(id));
                    if (doc != null) {
                        doc.setIsVerified(true);
                        doc.setIndexedAt(LocalDateTime.now());
                        searchService.indexListing(doc);
                        totalCount++;
                    }
                } catch (Exception ex) {
                    log.debug("Skip listing {}: {}", id, ex.getMessage());
                }
            }

            Boolean last = (Boolean) page.get("last");
            if (Boolean.TRUE.equals(last)) break;
            pageNum++;
        }

        return totalCount;
    }
}
