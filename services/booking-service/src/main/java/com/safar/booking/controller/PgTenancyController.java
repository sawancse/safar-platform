package com.safar.booking.controller;

import com.safar.booking.dto.CreateTenancyRequest;
import com.safar.booking.dto.TenantDashboardResponse;
import com.safar.booking.dto.TenantDashboardResponse.*;
import com.safar.booking.entity.PgTenancy;
import com.safar.booking.entity.TenancyAgreement;
import com.safar.booking.entity.TenancyInvoice;
import com.safar.booking.entity.enums.InvoiceStatus;
import com.safar.booking.entity.enums.MaintenanceStatus;
import com.safar.booking.entity.enums.TenancyStatus;
import com.safar.booking.repository.TenancyAgreementRepository;
import com.safar.booking.repository.MaintenanceRequestRepository;
import com.safar.booking.service.PgTenancyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pg-tenancies")
@RequiredArgsConstructor
public class PgTenancyController {

    private final PgTenancyService tenancyService;
    private final TenancyAgreementRepository agreementRepository;
    private final MaintenanceRequestRepository maintenanceRepository;

    @PostMapping
    public ResponseEntity<PgTenancy> createTenancy(@RequestBody CreateTenancyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tenancyService.createTenancyFromRequest(request));
    }

    @GetMapping
    public ResponseEntity<Page<PgTenancy>> getTenancies(
            @RequestParam(required = false) UUID listingId,
            @RequestParam(required = false) TenancyStatus status,
            @RequestParam(required = false) UUID tenantId,
            Pageable pageable) {
        return ResponseEntity.ok(tenancyService.getTenancies(listingId, status, tenantId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PgTenancy> getTenancy(@PathVariable UUID id) {
        return ResponseEntity.ok(tenancyService.getTenancy(id));
    }

    @PostMapping("/{id}/notice")
    public ResponseEntity<PgTenancy> giveNotice(@PathVariable UUID id) {
        return ResponseEntity.ok(tenancyService.giveNotice(id));
    }

    /**
     * Tenant gives 1-month notice to terminate their PG stay.
     * Terminates the agreement and sets move-out date = today + noticePeriodDays.
     */
    @PostMapping("/{id}/tenant-notice")
    public ResponseEntity<PgTenancy> tenantGiveNotice(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(tenancyService.tenantGiveNotice(id, userId));
    }

    @PostMapping("/{id}/vacate")
    public ResponseEntity<PgTenancy> vacate(@PathVariable UUID id) {
        return ResponseEntity.ok(tenancyService.vacate(id));
    }

    @PatchMapping("/{id}/penalty-config")
    public ResponseEntity<PgTenancy> updatePenaltyConfig(
            @PathVariable UUID id,
            @RequestParam(required = false) Integer gracePeriodDays,
            @RequestParam(required = false) Integer latePenaltyBps,
            @RequestParam(required = false) Integer maxPenaltyPercent) {
        return ResponseEntity.ok(tenancyService.updatePenaltyConfig(id, gracePeriodDays, latePenaltyBps, maxPenaltyPercent));
    }

    @GetMapping("/{id}/invoices")
    public ResponseEntity<Page<TenancyInvoice>> getInvoices(@PathVariable UUID id, Pageable pageable) {
        return ResponseEntity.ok(tenancyService.getInvoices(id, pageable));
    }

    @PostMapping("/invoices/{invoiceId}/pay")
    public ResponseEntity<TenancyInvoice> markPaid(
            @PathVariable UUID invoiceId,
            @RequestParam String razorpayPaymentId) {
        return ResponseEntity.ok(tenancyService.markInvoicePaid(invoiceId, razorpayPaymentId));
    }

    @GetMapping("/invoices/overdue")
    public ResponseEntity<List<TenancyInvoice>> getOverdueInvoices() {
        return ResponseEntity.ok(tenancyService.getOverdueInvoices());
    }

    @GetMapping("/my-dashboard")
    public ResponseEntity<TenantDashboardResponse> getTenantDashboard(
            @RequestHeader("X-User-Id") UUID userId) {
        // Find active tenancy for tenant
        Page<PgTenancy> tenancies = tenancyService.getTenancies(null, null, userId, PageRequest.of(0, 1));
        if (tenancies.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        PgTenancy tenancy = tenancies.getContent().get(0);

        // Tenancy snapshot
        TenancySnapshot tenancySnap = new TenancySnapshot(
                tenancy.getId(), tenancy.getTenancyRef(), tenancy.getStatus().name(),
                tenancy.getMoveInDate(), tenancy.getMoveOutDate(),
                tenancy.getMonthlyRentPaise(), tenancy.getSecurityDepositPaise(),
                tenancy.getNoticePeriodDays());

        // Agreement snapshot
        AgreementSnapshot agreementSnap = agreementRepository.findByTenancyId(tenancy.getId())
                .map(a -> new AgreementSnapshot(a.getStatus().name(), a.getAgreementNumber(), a.getAgreementPdfUrl()))
                .orElse(new AgreementSnapshot("NOT_CREATED", null, null));

        // Current invoice
        Page<TenancyInvoice> invoices = tenancyService.getInvoices(tenancy.getId(), PageRequest.of(0, 1));
        InvoiceSnapshot invoiceSnap = invoices.isEmpty() ? null :
                invoices.getContent().stream().findFirst()
                        .map(inv -> new InvoiceSnapshot(
                                inv.getId(), inv.getInvoiceNumber(), inv.getGrandTotalPaise(),
                                inv.getDueDate(), inv.getStatus().name()))
                        .orElse(null);

        // Totals
        long totalPaid = tenancyService.getInvoices(tenancy.getId(), Pageable.unpaged())
                .stream()
                .filter(inv -> inv.getStatus() == InvoiceStatus.PAID)
                .mapToLong(TenancyInvoice::getGrandTotalPaise)
                .sum();

        long outstanding = tenancyService.getInvoices(tenancy.getId(), Pageable.unpaged())
                .stream()
                .filter(inv -> inv.getStatus() == InvoiceStatus.GENERATED || inv.getStatus() == InvoiceStatus.OVERDUE)
                .mapToLong(TenancyInvoice::getGrandTotalPaise)
                .sum();

        int openMaintenance = (int) maintenanceRepository.countByTenancyIdAndStatus(
                tenancy.getId(), MaintenanceStatus.OPEN);

        // Subscription snapshot
        SubscriptionSnapshot subSnap = new SubscriptionSnapshot(
                tenancy.getSubscriptionStatus() != null ? tenancy.getSubscriptionStatus() : "NOT_CREATED",
                tenancy.getRazorpaySubscriptionId());

        return ResponseEntity.ok(new TenantDashboardResponse(
                tenancySnap, agreementSnap, invoiceSnap,
                totalPaid, outstanding, openMaintenance, subSnap));
    }
}
