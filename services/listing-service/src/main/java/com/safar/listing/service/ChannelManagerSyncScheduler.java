package com.safar.listing.service;

import com.safar.listing.entity.ChannelManagerProperty;
import com.safar.listing.entity.enums.ChannelStatus;
import com.safar.listing.repository.ChannelManagerPropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChannelManagerSyncScheduler {

    private final ChannelManagerPropertyRepository propertyRepository;
    private final ChannelManagerService channelManagerService;

    /**
     * Every 30 minutes: sync rates + availability for all connected properties,
     * then pull new bookings.
     */
    @Scheduled(fixedRate = 1800000) // 30 minutes
    public void syncAllConnectedProperties() {
        List<ChannelManagerProperty> connected = propertyRepository.findByStatus(ChannelStatus.CONNECTED);
        log.info("Channel manager sync: {} connected properties", connected.size());

        for (ChannelManagerProperty cmp : connected) {
            try {
                channelManagerService.syncRates(cmp.getListingId());
                channelManagerService.syncAvailability(cmp.getListingId());
                channelManagerService.pullBookings(cmp.getListingId());
            } catch (Exception e) {
                log.error("Sync failed for listing {}: {}", cmp.getListingId(), e.getMessage());
                // Don't stop — continue with other properties
                if (isConsecutiveFailure(cmp)) {
                    cmp.setStatus(ChannelStatus.ERROR);
                    propertyRepository.save(cmp);
                    log.warn("Listing {} marked as ERROR after consecutive failures", cmp.getListingId());
                }
            }
        }
    }

    private boolean isConsecutiveFailure(ChannelManagerProperty cmp) {
        // Simplified: would check sync_logs for consecutive failures
        return false;
    }
}
