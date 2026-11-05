package com.safar.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Daily batch: one payout per host covering every line past checkout + dispute window.
 * Does nothing unless {@code payouts.auto-execute=true} (default false → admin pays manually).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PayoutBatchScheduler {

    private final PayoutService payoutService;

    @Value("${payouts.auto-execute:false}")
    private boolean autoExecute;

    @Scheduled(cron = "${payouts.batch-cron:0 0 6 * * *}")
    public void run() {
        if (!autoExecute) {
            log.debug("Payout batch skipped (payouts.auto-execute=false)");
            return;
        }
        int sent = payoutService.executeAllEligible();
        log.info("Payout batch finished: {} host payouts sent", sent);
    }
}
