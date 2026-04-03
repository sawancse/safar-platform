package com.safar.payment.service;

import com.safar.payment.entity.HostPayout;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class HostPayoutScheduler {

    private final HostPayoutService hostPayoutService;

    /**
     * Daily at 2 AM: process any PENDING payouts that weren't auto-executed.
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void processPendingPayouts() {
        log.info("Starting pending payout processing");
        List<HostPayout> pending = hostPayoutService.getPendingPayouts();
        int processed = 0;
        int failed = 0;

        for (HostPayout payout : pending) {
            try {
                hostPayoutService.executeTransfer(payout.getId());
                processed++;
            } catch (Exception e) {
                failed++;
                log.error("Failed to process payout {}: {}", payout.getId(), e.getMessage());
            }
        }
        log.info("Pending payout run: {} processed, {} failed out of {} total", processed, failed, pending.size());
    }

    /**
     * Daily at 3 AM: retry FAILED payouts (max 3 retries).
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void retryFailedPayouts() {
        log.info("Starting failed payout retry");
        List<HostPayout> failed = hostPayoutService.getFailedPayouts();
        int retried = 0;

        for (HostPayout payout : failed) {
            if (payout.getRetryCount() >= 3) {
                log.warn("Payout {} exceeded max retries ({}), skipping", payout.getId(), payout.getRetryCount());
                continue;
            }
            try {
                hostPayoutService.retryPayout(payout.getId());
                retried++;
            } catch (Exception e) {
                log.error("Retry failed for payout {}: {}", payout.getId(), e.getMessage());
            }
        }
        log.info("Failed payout retry: {} retried out of {} total", retried, failed.size());
    }
}
