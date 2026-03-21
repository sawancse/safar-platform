package com.safar.listing.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ICalSyncScheduler {

    private final ICalService icalService;

    /**
     * Sync due iCal feeds every 15 minutes.
     * Each feed has its own sync interval (default 6 hours).
     * Failing feeds use exponential backoff (15min, 30min, 1h, 2h, ... max 24h).
     * Feeds deactivated after 10 consecutive failures.
     */
    @Scheduled(fixedDelay = 900_000) // 15 minutes
    public void syncDueFeeds() {
        try {
            icalService.syncDueFeeds();
        } catch (Exception e) {
            log.error("Error in scheduled iCal sync: {}", e.getMessage(), e);
        }
    }
}
