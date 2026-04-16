package com.safar.booking.service;

import com.safar.booking.entity.PgTenancy;
import com.safar.booking.entity.TenancyInvoice;
import com.safar.booking.entity.enums.InvoiceStatus;
import com.safar.booking.entity.enums.TenancyStatus;
import com.safar.booking.kafka.KafkaJsonPublisher;
import com.safar.booking.repository.PgTenancyRepository;
import com.safar.booking.repository.TenancyInvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class TenancyBillingScheduler {

    private final PgTenancyService tenancyService;
    private final PgTenancyRepository tenancyRepository;
    private final TenancyInvoiceRepository invoiceRepository;
    private final KafkaJsonPublisher kafkaJsonPublisher;

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
                    // Enrich event with tenancy context for notification-service
                    Map<String, Object> overdueEvent = new HashMap<>();
                    overdueEvent.put("id", invoice.getId().toString());
                    overdueEvent.put("tenancyId", invoice.getTenancyId().toString());
                    overdueEvent.put("tenantId", tenancy.getTenantId().toString());
                    overdueEvent.put("tenancyRef", tenancy.getTenancyRef());
                    overdueEvent.put("invoiceNumber", invoice.getInvoiceNumber());
                    overdueEvent.put("grandTotalPaise", invoice.getGrandTotalPaise());
                    overdueEvent.put("dueDate", invoice.getDueDate().toString());
                    overdueEvent.put("latePenaltyPaise", penalty);
                    kafkaJsonPublisher.publish("tenancy.invoice.overdue", invoice.getId().toString(), overdueEvent);
                }
                invoiceRepository.save(invoice);
                penalized++;
            }
        }
        log.info("Late penalty run completed: {} invoices penalized", penalized);
    }

    /**
     * Daily at 8 AM IST: send 7-day advance rent reminder BEFORE invoice is generated.
     * Finds ACTIVE tenancies whose nextBillingDate is exactly 7 days away and haven't been reminded yet.
     * This gives tenants a week's notice to keep funds ready.
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void sendAdvanceRentReminders() {
        log.info("Starting 7-day advance rent reminder run");
        LocalDate targetBillingDate = LocalDate.now().plusDays(7);

        List<PgTenancy> upcoming = tenancyRepository.findByStatusAndNextBillingDate(
                TenancyStatus.ACTIVE, targetBillingDate);

        int sent = 0;
        for (PgTenancy tenancy : upcoming) {
            if (tenancy.isRentAdvanceReminderSent()) continue;
            try {
                Map<String, Object> event = new HashMap<>();
                event.put("tenantId", tenancy.getTenantId().toString());
                event.put("tenancyId", tenancy.getId().toString());
                event.put("tenancyRef", tenancy.getTenancyRef());
                event.put("listingId", tenancy.getListingId().toString());
                event.put("monthlyRentPaise", tenancy.getMonthlyRentPaise());
                event.put("totalMonthlyPaise", tenancy.getTotalMonthlyPaise());
                event.put("nextBillingDate", tenancy.getNextBillingDate().toString());
                event.put("daysUntilBilling", 7);

                kafkaJsonPublisher.publish("tenancy.rent.reminder.advance", tenancy.getId().toString(), event);
                tenancy.setRentAdvanceReminderSent(true);
                tenancyRepository.save(tenancy);
                sent++;
            } catch (Exception e) {
                log.error("Failed to send advance reminder for tenancy {}: {}", tenancy.getTenancyRef(), e.getMessage());
            }
        }
        log.info("7-day advance reminders sent for {}/{} tenancies", sent, upcoming.size());
    }

    /**
     * Daily at 9 AM IST: send 5-day-before rent payment reminders.
     * Finds invoices due in 5 days that haven't received this reminder yet.
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void sendPreDueReminders() {
        log.info("Starting 5-day pre-due payment reminder run");
        LocalDate reminderTarget = LocalDate.now().plusDays(5);

        List<TenancyInvoice> upcoming = invoiceRepository.findByDueDateAndStatusInAndReminder5dSentFalse(
                reminderTarget, List.of(InvoiceStatus.GENERATED, InvoiceStatus.SENT));

        int sent = 0;
        for (TenancyInvoice invoice : upcoming) {
            try {
                Map<String, Object> event = buildReminderEvent(invoice, 5);
                kafkaJsonPublisher.publish("tenancy.rent.reminder", invoice.getId().toString(), event);
                invoice.setReminder5dSent(true);
                invoiceRepository.save(invoice);
                sent++;
            } catch (Exception e) {
                log.error("Failed to send 5-day reminder for invoice {}: {}", invoice.getInvoiceNumber(), e.getMessage());
            }
        }
        log.info("5-day reminders sent for {}/{} invoices", sent, upcoming.size());
    }

    /**
     * Daily at 9:30 AM IST: send 1-day-before urgent rent payment reminders.
     */
    @Scheduled(cron = "0 30 9 * * *")
    public void sendUrgentReminders() {
        log.info("Starting 1-day urgent payment reminder run");
        LocalDate reminderTarget = LocalDate.now().plusDays(1);

        List<TenancyInvoice> upcoming = invoiceRepository.findByDueDateAndStatusInAndReminder1dSentFalse(
                reminderTarget, List.of(InvoiceStatus.GENERATED, InvoiceStatus.SENT));

        int sent = 0;
        for (TenancyInvoice invoice : upcoming) {
            try {
                Map<String, Object> event = buildReminderEvent(invoice, 1);
                kafkaJsonPublisher.publish("tenancy.rent.reminder.urgent", invoice.getId().toString(), event);
                invoice.setReminder1dSent(true);
                invoiceRepository.save(invoice);
                sent++;
            } catch (Exception e) {
                log.error("Failed to send urgent reminder for invoice {}: {}", invoice.getInvoiceNumber(), e.getMessage());
            }
        }
        log.info("Urgent reminders sent for {}/{} invoices", sent, upcoming.size());
    }

    private Map<String, Object> buildReminderEvent(TenancyInvoice invoice, int daysUntilDue) {
        PgTenancy tenancy = tenancyRepository.findById(invoice.getTenancyId()).orElse(null);
        Map<String, Object> event = new HashMap<>();
        event.put("invoiceId", invoice.getId().toString());
        event.put("invoiceNumber", invoice.getInvoiceNumber());
        event.put("tenancyId", invoice.getTenancyId().toString());
        event.put("grandTotalPaise", invoice.getGrandTotalPaise());
        event.put("rentPaise", invoice.getRentPaise());
        event.put("dueDate", invoice.getDueDate().toString());
        event.put("daysUntilDue", daysUntilDue);
        event.put("billingMonth", invoice.getBillingMonth());
        event.put("billingYear", invoice.getBillingYear());
        if (tenancy != null) {
            event.put("tenantId", tenancy.getTenantId().toString());
            event.put("listingId", tenancy.getListingId().toString());
            event.put("tenancyRef", tenancy.getTenancyRef());
            event.put("monthlyRentPaise", tenancy.getMonthlyRentPaise());
        }
        return event;
    }
}
