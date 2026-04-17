package com.safar.booking.service;

import com.safar.booking.dto.CreateTenancyRequest;
import com.safar.booking.entity.PgTenancy;
import com.safar.booking.entity.TenancyAgreement;
import com.safar.booking.entity.TenancyInvoice;
import com.safar.booking.entity.enums.AgreementStatus;
import com.safar.booking.entity.enums.InvoiceStatus;
import com.safar.booking.entity.enums.TenancyStatus;
import com.safar.booking.repository.PgTenancyRepository;
import com.safar.booking.repository.TenancyAgreementRepository;
import com.safar.booking.repository.TenancyInvoiceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;

import java.time.LocalDate;
import java.time.Year;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class PgTenancyService {

    private final PgTenancyRepository tenancyRepository;
    private final TenancyInvoiceRepository invoiceRepository;
    private final TenancyAgreementRepository agreementRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final com.safar.booking.kafka.KafkaJsonPublisher kafkaJsonPublisher;
    private final UtilityReadingService utilityReadingService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String listingServiceUrl;

    public PgTenancyService(PgTenancyRepository tenancyRepository,
                            TenancyInvoiceRepository invoiceRepository,
                            TenancyAgreementRepository agreementRepository,
                            KafkaTemplate<String, Object> kafkaTemplate,
                            com.safar.booking.kafka.KafkaJsonPublisher kafkaJsonPublisher,
                            UtilityReadingService utilityReadingService,
                            RestTemplate restTemplate,
                            ObjectMapper objectMapper,
                            @Value("${services.listing-service.url}") String listingServiceUrl) {
        this.tenancyRepository = tenancyRepository;
        this.invoiceRepository = invoiceRepository;
        this.agreementRepository = agreementRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaJsonPublisher = kafkaJsonPublisher;
        this.utilityReadingService = utilityReadingService;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.listingServiceUrl = listingServiceUrl;
    }

    private static long tenancyCounter = 1000;
    private static long invoiceCounter = 1000;

    /**
     * Seed counters from DB on startup so they survive restarts.
     * Prior bug: static counters reset to 1000 on each restart → duplicate key
     * on PGT-2026-1005 / INV-PG-2026-1005 once DB already held those refs.
     */
    @PostConstruct
    void seedCountersFromDb() {
        int year = Year.now().getValue();
        String tPrefix = "PGT-" + year + "-";
        tenancyRepository.findMaxTenancyRefLike(tPrefix + "%").ifPresent(ref -> {
            try {
                long n = Long.parseLong(ref.substring(tPrefix.length()));
                if (n >= tenancyCounter) tenancyCounter = n;
            } catch (NumberFormatException ignored) {}
        });
        String iPrefix = "INV-PG-" + year + "-";
        invoiceRepository.findMaxInvoiceNumberLike(iPrefix + "%").ifPresent(num -> {
            try {
                long n = Long.parseLong(num.substring(iPrefix.length()));
                if (n >= invoiceCounter) invoiceCounter = n;
            } catch (NumberFormatException ignored) {}
        });
        log.info("Tenancy/invoice counters seeded: tenancy={}, invoice={}", tenancyCounter, invoiceCounter);
    }

    /**
     * Create tenancy from DTO, inheriting penalty config from listing defaults.
     * Priority: request value > listing value > hardcoded default.
     */
    @Transactional
    public PgTenancy createTenancyFromRequest(CreateTenancyRequest req) {
        // Fetch listing defaults for penalty config
        int listingGraceDays = 5;
        int listingPenaltyBps = 200;
        int listingMaxPenaltyPercent = 25;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> listing = restTemplate.getForObject(
                    listingServiceUrl + "/api/v1/listings/" + req.listingId(), Map.class);
            if (listing != null) {
                if (listing.get("gracePeriodDays") != null)
                    listingGraceDays = ((Number) listing.get("gracePeriodDays")).intValue();
                if (listing.get("latePenaltyBps") != null)
                    listingPenaltyBps = ((Number) listing.get("latePenaltyBps")).intValue();
            }
        } catch (Exception e) {
            log.warn("Could not fetch listing {} for penalty defaults, using hardcoded: {}",
                    req.listingId(), e.getMessage());
        }

        PgTenancy tenancy = PgTenancy.builder()
                .tenantId(req.tenantId())
                .listingId(req.listingId())
                .roomTypeId(req.roomTypeId())
                .bedNumber(req.bedNumber())
                .sharingType(req.sharingType() != null ? req.sharingType() : "PRIVATE")
                .moveInDate(req.moveInDate())
                .noticePeriodDays(req.noticePeriodDays())
                .monthlyRentPaise(req.monthlyRentPaise())
                .securityDepositPaise(req.securityDepositPaise())
                .mealsIncluded(req.mealsIncluded())
                .laundryIncluded(req.laundryIncluded())
                .wifiIncluded(req.wifiIncluded())
                .totalMonthlyPaise(req.totalMonthlyPaise())
                .billingDay(req.billingDay() != null ? req.billingDay() : 1)
                .gracePeriodDays(req.gracePeriodDays() != null ? req.gracePeriodDays() : listingGraceDays)
                .latePenaltyBps(req.latePenaltyBps() != null ? req.latePenaltyBps() : listingPenaltyBps)
                .maxPenaltyPercent(req.maxPenaltyPercent() != null ? req.maxPenaltyPercent() : listingMaxPenaltyPercent)
                .build();

        return createTenancy(tenancy);
    }

    @Transactional
    public PgTenancy createTenancy(PgTenancy tenancy) {
        // Preflight bed-availability check — avoids taking payment on a room that is
        // already full. The Kafka consumer in listing-service would otherwise throw
        // IllegalStateException("Cannot occupy N bed(s) — only 0 available") after
        // the customer has already been charged.
        if (tenancy.getRoomTypeId() != null) {
            assertBedsAvailable(tenancy.getRoomTypeId(), tenancy.getSharingType());
        }

        tenancy.setTenancyRef("PGT-" + Year.now().getValue() + "-" + String.format("%04d", ++tenancyCounter));
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
        kafkaTemplate.send("tenancy.created", saved.getId().toString(), toEventJson(saved));
        log.info("PG tenancy created: {} for listing {}", saved.getTenancyRef(), saved.getListingId());
        return saved;
    }

    public List<PgTenancy> getActiveTenanciesByRoomType(UUID roomTypeId) {
        return tenancyRepository.findByRoomTypeIdAndStatus(roomTypeId, TenancyStatus.ACTIVE);
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
        kafkaTemplate.send("tenancy.notice", saved.getId().toString(), toEventJson(saved));
        log.info("Notice given for tenancy {}, move-out: {}", saved.getTenancyRef(), saved.getMoveOutDate());

        // Terminate associated agreement
        agreementRepository.findByTenancyId(id).ifPresent(agreement -> {
            if (agreement.getStatus() == AgreementStatus.ACTIVE
                    || agreement.getStatus() == AgreementStatus.PENDING_TENANT_SIGN
                    || agreement.getStatus() == AgreementStatus.PENDING_HOST_SIGN) {
                agreement.setStatus(AgreementStatus.TERMINATED);
                agreementRepository.save(agreement);
                kafkaTemplate.send("tenancy.agreement.terminated", id.toString(), toEventJson(saved));
                log.info("Agreement {} terminated due to notice on tenancy {}",
                        agreement.getAgreementNumber(), saved.getTenancyRef());
            }
        });

        return saved;
    }

    /**
     * Tenant gives 1-month notice to terminate their PG stay.
     * Validates that the caller is the actual tenant.
     */
    @Transactional
    public PgTenancy tenantGiveNotice(UUID tenancyId, UUID tenantId) {
        PgTenancy tenancy = getTenancy(tenancyId);
        if (!tenancy.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("Only the tenant can give notice on their tenancy");
        }
        return giveNotice(tenancyId);
    }

    @Transactional
    public PgTenancy vacate(UUID id) {
        PgTenancy tenancy = getTenancy(id);
        tenancy.setStatus(TenancyStatus.VACATED);
        if (tenancy.getMoveOutDate() == null) {
            tenancy.setMoveOutDate(LocalDate.now());
        }
        PgTenancy saved = tenancyRepository.save(tenancy);
        kafkaTemplate.send("tenancy.vacated", saved.getId().toString(), toEventJson(saved));
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

        // Include unbilled utility charges
        long electricity = utilityReadingService.getUnbilledElectricity(tenancy.getId());
        long water = utilityReadingService.getUnbilledWater(tenancy.getId());

        long total = rent + packages + electricity + water;
        long gst = total * 18 / 100; // 18% GST
        long grandTotal = total + gst;

        TenancyInvoice invoice = TenancyInvoice.builder()
                .tenancyId(tenancy.getId())
                .tenantId(tenancy.getTenantId())
                .invoiceNumber("INV-PG-" + year + "-" + String.format("%04d", ++invoiceCounter))
                .billingMonth(month)
                .billingYear(year)
                .rentPaise(rent)
                .packagesPaise(packages)
                .electricityPaise(electricity)
                .waterPaise(water)
                .totalPaise(total)
                .gstPaise(gst)
                .grandTotalPaise(grandTotal)
                .status(InvoiceStatus.GENERATED)
                .dueDate(tenancy.getNextBillingDate().plusDays(7))
                .build();

        TenancyInvoice saved = invoiceRepository.save(invoice);

        // Mark utility readings as billed
        if (electricity > 0 || water > 0) {
            utilityReadingService.markBilled(tenancy.getId(), saved.getId());
        }

        // Enrich event with tenancy context so notification-service has tenantId & tenancyRef
        java.util.Map<String, Object> invoiceEvent = new java.util.HashMap<>();
        invoiceEvent.put("id", saved.getId().toString());
        invoiceEvent.put("tenancyId", saved.getTenancyId().toString());
        invoiceEvent.put("tenantId", tenancy.getTenantId().toString());
        invoiceEvent.put("tenancyRef", tenancy.getTenancyRef());
        invoiceEvent.put("invoiceNumber", saved.getInvoiceNumber());
        invoiceEvent.put("billingMonth", saved.getBillingMonth());
        invoiceEvent.put("billingYear", saved.getBillingYear());
        invoiceEvent.put("rentPaise", saved.getRentPaise());
        invoiceEvent.put("grandTotalPaise", saved.getGrandTotalPaise());
        invoiceEvent.put("dueDate", saved.getDueDate().toString());
        invoiceEvent.put("status", saved.getStatus().name());
        kafkaJsonPublisher.publish("tenancy.invoice.generated", saved.getId().toString(), invoiceEvent);
        log.info("Invoice {} generated for tenancy {} (rent={}, pkg={}, elec={}, water={})",
                saved.getInvoiceNumber(), tenancy.getTenancyRef(), rent, packages, electricity, water);
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
                // Advance next billing date and reset advance reminder flag for next cycle
                tenancy.setNextBillingDate(tenancy.getNextBillingDate().plusMonths(1));
                tenancy.setRentAdvanceReminderSent(false);
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

    @Transactional
    public PgTenancy updatePenaltyConfig(UUID id, Integer gracePeriodDays, Integer latePenaltyBps, Integer maxPenaltyPercent) {
        PgTenancy tenancy = getTenancy(id);
        if (gracePeriodDays != null) {
            if (gracePeriodDays < 0 || gracePeriodDays > 30) {
                throw new RuntimeException("Grace period must be 0-30 days");
            }
            tenancy.setGracePeriodDays(gracePeriodDays);
        }
        if (latePenaltyBps != null) {
            if (latePenaltyBps < 0 || latePenaltyBps > 500) {
                throw new RuntimeException("Late penalty must be 0-500 bps (0-5% per day)");
            }
            tenancy.setLatePenaltyBps(latePenaltyBps);
        }
        if (maxPenaltyPercent != null) {
            if (maxPenaltyPercent < 0 || maxPenaltyPercent > 100) {
                throw new RuntimeException("Max penalty percent must be 0-100");
            }
            tenancy.setMaxPenaltyPercent(maxPenaltyPercent);
        }
        PgTenancy saved = tenancyRepository.save(tenancy);
        log.info("Penalty config updated for tenancy {}: grace={}d, penalty={}bps, maxCap={}%",
                saved.getTenancyRef(), saved.getGracePeriodDays(), saved.getLatePenaltyBps(), saved.getMaxPenaltyPercent());
        return saved;
    }

    /**
     * Convert tenancy to a JSON string for Kafka events, including fields
     * needed by listing-service to manage room occupancy.
     */
    /**
     * Calls listing-service to ensure the room type has beds free for this tenancy.
     * Throws {@link IllegalStateException} (mapped to 409 by the advice) if full.
     *
     * NOT annotated @Transactional on purpose — callers that already hold a TX must
     * invoke this BEFORE entering the TX, otherwise an exception here would mark
     * the outer TX rollback-only even if the caller catches it.
     */
    @SuppressWarnings("unchecked")
    public void assertBedsAvailable(UUID roomTypeId, String sharingType) {
        try {
            Map<String, Object> rt = restTemplate.getForObject(
                    listingServiceUrl + "/api/v1/internal/room-types/" + roomTypeId, Map.class);
            if (rt == null) return; // soft-fail — downstream consumer will surface error
            int total = rt.get("totalBeds") != null ? ((Number) rt.get("totalBeds")).intValue()
                    : (rt.get("count") != null ? ((Number) rt.get("count")).intValue() : 0);
            int occupied = rt.get("occupiedBeds") != null ? ((Number) rt.get("occupiedBeds")).intValue() : 0;
            int needed = "PRIVATE".equals(sharingType) || sharingType == null ? Math.max(1, total) : 1;
            // For PRIVATE we need a whole empty room; bed count is therefore total/count
            // but the simplest correctness check is: requested beds must fit in free beds.
            int free = total - occupied;
            if (free < needed) {
                throw new IllegalStateException("No beds available in this room type — "
                        + occupied + "/" + total + " occupied. Please pick a different room.");
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Bed availability pre-check failed for room type {}: {}. Proceeding; listing-service consumer will validate.",
                    roomTypeId, e.getMessage());
        }
    }

    private String toEventJson(PgTenancy tenancy) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("id", tenancy.getId().toString());
            event.put("tenancyRef", tenancy.getTenancyRef());
            event.put("tenantId", tenancy.getTenantId().toString());
            event.put("listingId", tenancy.getListingId().toString());
            if (tenancy.getRoomTypeId() != null) {
                event.put("roomTypeId", tenancy.getRoomTypeId().toString());
            }
            event.put("bedNumber", tenancy.getBedNumber());
            event.put("sharingType", tenancy.getSharingType());
            event.put("status", tenancy.getStatus().name());
            event.put("moveInDate", tenancy.getMoveInDate() != null ? tenancy.getMoveInDate().toString() : null);
            event.put("moveOutDate", tenancy.getMoveOutDate() != null ? tenancy.getMoveOutDate().toString() : null);
            event.put("monthlyRentPaise", tenancy.getMonthlyRentPaise());
            return objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            log.error("Failed to serialize tenancy event: {}", e.getMessage());
            return "{}";
        }
    }
}
