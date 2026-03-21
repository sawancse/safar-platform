package com.safar.booking.service;

import com.safar.booking.entity.PgTenancy;
import com.safar.booking.entity.TenancyInvoice;
import com.safar.booking.entity.enums.InvoiceStatus;
import com.safar.booking.entity.enums.TenancyStatus;
import com.safar.booking.repository.PgTenancyRepository;
import com.safar.booking.repository.TenancyInvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PgTenancyService {

    private final PgTenancyRepository tenancyRepository;
    private final TenancyInvoiceRepository invoiceRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static long tenancyCounter = 1000;
    private static long invoiceCounter = 1000;

    @Transactional
    public PgTenancy createTenancy(PgTenancy tenancy) {
        tenancy.setTenancyRef("PGT-2026-" + String.format("%04d", ++tenancyCounter));
        tenancy.setStatus(TenancyStatus.ACTIVE);

        // Calculate next billing date
        LocalDate moveIn = tenancy.getMoveInDate();
        int billingDay = tenancy.getBillingDay();
        LocalDate nextBilling;
        if (moveIn.getDayOfMonth() <= billingDay) {
            nextBilling = moveIn.withDayOfMonth(Math.min(billingDay, moveIn.lengthOfMonth()));
        } else {
            nextBilling = moveIn.plusMonths(1).withDayOfMonth(
                    Math.min(billingDay, moveIn.plusMonths(1).lengthOfMonth()));
        }
        tenancy.setNextBillingDate(nextBilling);

        PgTenancy saved = tenancyRepository.save(tenancy);
        kafkaTemplate.send("tenancy.created", saved.getId().toString(), saved);
        log.info("PG tenancy created: {} for listing {}", saved.getTenancyRef(), saved.getListingId());
        return saved;
    }

    public PgTenancy getTenancy(UUID id) {
        return tenancyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("PG tenancy not found: " + id));
    }

    public Page<PgTenancy> getTenancies(UUID listingId, TenancyStatus status, UUID tenantId, Pageable pageable) {
        if (listingId != null && status != null) {
            return tenancyRepository.findByListingIdAndStatus(listingId, status, pageable);
        } else if (listingId != null) {
            return tenancyRepository.findByListingId(listingId, pageable);
        } else if (tenantId != null) {
            return tenancyRepository.findByTenantId(tenantId, pageable);
        }
        return tenancyRepository.findAll(pageable);
    }

    @Transactional
    public PgTenancy giveNotice(UUID id) {
        PgTenancy tenancy = getTenancy(id);
        if (tenancy.getStatus() != TenancyStatus.ACTIVE) {
            throw new RuntimeException("Can only give notice on active tenancy");
        }
        tenancy.setStatus(TenancyStatus.NOTICE_PERIOD);
        tenancy.setMoveOutDate(LocalDate.now().plusDays(tenancy.getNoticePeriodDays()));
        PgTenancy saved = tenancyRepository.save(tenancy);
        kafkaTemplate.send("tenancy.notice", saved.getId().toString(), saved);
        log.info("Notice given for tenancy {}, move-out: {}", saved.getTenancyRef(), saved.getMoveOutDate());
        return saved;
    }

    @Transactional
    public PgTenancy vacate(UUID id) {
        PgTenancy tenancy = getTenancy(id);
        tenancy.setStatus(TenancyStatus.VACATED);
        if (tenancy.getMoveOutDate() == null) {
            tenancy.setMoveOutDate(LocalDate.now());
        }
        PgTenancy saved = tenancyRepository.save(tenancy);
        kafkaTemplate.send("tenancy.vacated", saved.getId().toString(), saved);
        log.info("Tenancy {} vacated", saved.getTenancyRef());
        return saved;
    }

    @Transactional
    public TenancyInvoice generateInvoice(PgTenancy tenancy) {
        int month = tenancy.getNextBillingDate().getMonthValue();
        int year = tenancy.getNextBillingDate().getYear();

        if (invoiceRepository.existsByTenancyIdAndBillingMonthAndBillingYear(
                tenancy.getId(), month, year)) {
            throw new RuntimeException("Invoice already exists for this period");
        }

        long rent = tenancy.getMonthlyRentPaise();
        long packages = tenancy.getTotalMonthlyPaise() - rent;
        long total = rent + packages;
        long gst = total * 18 / 100; // 18% GST
        long grandTotal = total + gst;

        TenancyInvoice invoice = TenancyInvoice.builder()
                .tenancyId(tenancy.getId())
                .invoiceNumber("INV-PG-" + year + "-" + String.format("%04d", ++invoiceCounter))
                .billingMonth(month)
                .billingYear(year)
                .rentPaise(rent)
                .packagesPaise(packages)
                .totalPaise(total)
                .gstPaise(gst)
                .grandTotalPaise(grandTotal)
                .status(InvoiceStatus.GENERATED)
                .dueDate(tenancy.getNextBillingDate().plusDays(7))
                .build();

        TenancyInvoice saved = invoiceRepository.save(invoice);
        kafkaTemplate.send("tenancy.invoice.generated", saved.getId().toString(), saved);
        log.info("Invoice {} generated for tenancy {}", saved.getInvoiceNumber(), tenancy.getTenancyRef());
        return saved;
    }

    /**
     * Called by scheduler: generate invoices for all tenancies due today.
     */
    @Transactional
    public void generateMonthlyInvoices() {
        LocalDate today = LocalDate.now();
        List<PgTenancy> due = tenancyRepository.findByStatusAndNextBillingDate(
                TenancyStatus.ACTIVE, today);

        log.info("Generating invoices for {} tenancies due on {}", due.size(), today);

        for (PgTenancy tenancy : due) {
            try {
                generateInvoice(tenancy);
                // Advance next billing date
                tenancy.setNextBillingDate(tenancy.getNextBillingDate().plusMonths(1));
                tenancyRepository.save(tenancy);
            } catch (Exception e) {
                log.error("Failed to generate invoice for tenancy {}: {}",
                        tenancy.getTenancyRef(), e.getMessage());
            }
        }
    }

    @Transactional
    public TenancyInvoice markInvoicePaid(UUID invoiceId, String razorpayPaymentId) {
        TenancyInvoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found: " + invoiceId));
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidDate(LocalDate.now());
        invoice.setRazorpayPaymentId(razorpayPaymentId);
        return invoiceRepository.save(invoice);
    }

    public Page<TenancyInvoice> getInvoices(UUID tenancyId, Pageable pageable) {
        return invoiceRepository.findByTenancyIdOrderByBillingYearDescBillingMonthDesc(tenancyId, pageable);
    }

    public List<TenancyInvoice> getOverdueInvoices() {
        return invoiceRepository.findByStatus(InvoiceStatus.OVERDUE);
    }
}
