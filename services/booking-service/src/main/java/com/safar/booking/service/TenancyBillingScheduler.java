package com.safar.booking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TenancyBillingScheduler {

    private final PgTenancyService tenancyService;

    /**
     * Daily at 6 AM IST: generate invoices for all tenancies with nextBillingDate = today.
     */
    @Scheduled(cron = "0 0 6 * * *")
    public void dailyBillingRun() {
        log.info("Starting daily PG tenancy billing run");
        try {
            tenancyService.generateMonthlyInvoices();
            log.info("Daily billing run completed");
        } catch (Exception e) {
            log.error("Daily billing run failed: {}", e.getMessage(), e);
        }
    }
}
