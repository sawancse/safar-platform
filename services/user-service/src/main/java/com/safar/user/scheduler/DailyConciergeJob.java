package com.safar.user.scheduler;

import com.safar.user.entity.enums.GuestSubStatus;
import com.safar.user.repository.GuestSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DailyConciergeJob {

    private final GuestSubscriptionRepository subRepo;
    private final KafkaTemplate<String, String> kafka;

    /** Fires at 7:30am IST every day */
    @Scheduled(cron = "0 30 7 * * *", zone = "Asia/Kolkata")
    public void sendDailySuggestions() {
        var active = subRepo.findAllByStatus(GuestSubStatus.ACTIVE);
        log.info("Daily concierge: notifying {} Traveler Pro guests", active.size());
        active.forEach(sub -> {
            try {
                kafka.send("concierge.daily", sub.getGuestId().toString(),
                        sub.getGuestId().toString());
                log.debug("Concierge event queued for guest {}", sub.getGuestId());
            } catch (Exception e) {
                log.error("Failed to queue concierge for guest {}", sub.getGuestId(), e);
            }
        });
    }
}
