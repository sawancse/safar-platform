package com.safar.listing.service;

import com.safar.listing.dto.DemandAlertDto;
import com.safar.listing.entity.Listing;
import com.safar.listing.entity.enums.ListingStatus;
import com.safar.listing.repository.ListingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DemandAlertJob {

    private final ListingRepository listingRepository;
    private final DemandAlertService demandAlertService;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(cron = "0 0 8 * * *")
    public void sendDailyDemandAlerts() {
        log.info("Running daily demand alert job");
        List<Listing> activeListings = listingRepository.findByStatus(ListingStatus.VERIFIED);

        for (Listing listing : activeListings) {
            try {
                LocalDate tomorrow = LocalDate.now().plusDays(1);
                double maxMultiplier = 1.0;

                for (int i = 0; i < 7; i++) {
                    LocalDate date = tomorrow.plusDays(i);
                    double m = demandAlertService.computeDemandMultiplier(listing.getCity(), date);
                    if (m > maxMultiplier) {
                        maxMultiplier = m;
                    }
                }

                String alertType = demandAlertService.getAlertType(maxMultiplier);

                // Only send non-trivial alerts
                if (!"PRICE_OPPORTUNITY".equals(alertType)) {
                    DemandAlertDto alert = demandAlertService.computeAlert(listing.getId());
                    String payload = String.format(
                            "{\"listingId\":\"%s\",\"hostId\":\"%s\",\"alertType\":\"%s\",\"demandMultiplier\":%.2f,\"suggestedPricePaise\":%d}",
                            listing.getId(), listing.getHostId(), alert.alertType(),
                            alert.demandMultiplier(), alert.suggestedPricePaise());
                    kafkaTemplate.send("notification.host.demand_alert", listing.getId().toString(), payload);
                    log.info("Demand alert sent for listing {}: {}", listing.getId(), alertType);
                }
            } catch (Exception e) {
                log.error("Error processing demand alert for listing {}: {}", listing.getId(), e.getMessage());
            }
        }

        log.info("Daily demand alert job completed");
    }
}
