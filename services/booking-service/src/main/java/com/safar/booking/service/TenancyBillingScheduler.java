package com.safar.booking.service;

import com.safar.booking.entity.PgTenancy;
import com.safar.booking.entity.TenancyInvoice;
import com.safar.booking.entity.enums.InvoiceStatus;
import com.safar.booking.entity.enums.TenancyStatus;
import com.safar.booking.repository.PgTenancyRepository;
import com.safar.booking.repository.TenancyInvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TenancyBillingScheduler {

    private final PgTenancyService tenancyService;
    private final PgTenancyRepository tenancyRepository;
    private final TenancyInvoiceRepository invoiceRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Daily at 5 AM IST: auto-vacate tenancies in NOTICE_PERIOD whose moveOutDate has passed.
     * Publishes tenancy.vacated event which triggers room occupancy release in listing-service.
     */
    @Scheduled(cron = "0 0 5 * * *")
    public void autoVacateExpiredTenancies() {
        log.info("Starting auto-vacate check for expired tenancies");
        LocalDate today = LocalDate.now();

        List<PgTenancy> expired = tenancyRepository
                .findByStatusAndMoveOutDateLessThanEqual(TenancyStatus.NOTICE_PERIOD, today);

        int vacated = 0;
        for (PgTenancy tenancy : expired) {
            try {
                tenancyService.vacate(tenancy.getId());
                vacated++;
                log.info("Auto-vacated tenancy {} (moveOutDate={})", tenancy.getTenancyRef(), tenancy.getMoveOutDate());
            } catch (Exception e) {
                log.error("Failed to auto-vacate tenancy {}: {}", tenancy.getTenancyRef(), e.getMessage());
            }
        }
        log.info("Auto-vacate check completed: {}/{} tenancies vacated", vacated, expired.size());
    }

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

    /**
     * Daily at 7 AM IST: apply late payment penalties on overdue invoices.
     * Penalty = grandTotalPaise * latePenaltyBps / 10000 per day, applied after grace period.
     */
    @Scheduled(cron = "0 0 7 * * *")
    public void applyLatePenalties() {
        log.info("Starting late payment penalty run");
        LocalDate today = LocalDate.now();

        List<PgTenancy> activeTenancies = tenancyRepository.findByStatusAndNextBillingDate(
                TenancyStatus.ACTIVE, today);
        // Also process all ACTIVE and NOTICE_PERIOD tenancies
        List<PgTenancy> allActive = tenancyRepository.findAll().stream()
                .filter(t -> t.getStatus() == TenancyStatus.ACTIVE || t.getStatus() == TenancyStatus.NOTICE_PERIOD)
                .toList();

        int penalized = 0;
        for (PgTenancy tenancy : allActive) {
            List<TenancyInvoice> unpaid = invoiceRepository
                    .findByTenancyIdOrderByBillingYearDescBillingMonthDesc(tenancy.getId(), null)
                    .stream()
                    .filter(inv -> inv.getStatus() == InvoiceStatus.GENERATED || inv.getStatus() == InvoiceStatus.OVERDUE)
                    .filter(inv -> inv.getDueDate().plusDays(tenancy.getGracePeriodDays()).isBefore(today))
                    .toList();

            for (TenancyInvoice invoice : unpaid) {
                long daysOverdue = ChronoUnit.DAYS.between(
                        invoice.getDueDate().plusDays(tenancy.getGracePeriodDays()), today);
                if (daysOverdue <= 0) continue;

                long penalty = invoice.getGrandTotalPaise() * tenancy.getLatePenaltyBps() * daysOverdue / 10000;
                // Apply max penalty cap
                if (tenancy.getMaxPenaltyPercent() > 0) {
                    long maxPenalty = invoice.getGrandTotalPaise() * tenancy.getMaxPenaltyPercent() / 100;
                    penalty = Math.min(penalty, maxPenalty);
                }
                invoice.setLatePenaltyPaise(penalty);
                if (invoice.getStatus() != InvoiceStatus.OVERDUE) {
                    invoice.setStatus(InvoiceStatus.OVERDUE);
                    kafkaTemplate.send("tenancy.invoice.overdue", invoice.getId().toString(), invoice);
                }
                invoiceRepository.save(invoice);
                penalized++;
            }
        }
        log.info("Late penalty run completed: {} invoices penalized", penalized);
    }
}
