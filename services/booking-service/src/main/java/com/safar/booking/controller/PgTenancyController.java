package com.safar.booking.controller;

import com.safar.booking.entity.PgTenancy;
import com.safar.booking.entity.TenancyInvoice;
import com.safar.booking.entity.enums.TenancyStatus;
import com.safar.booking.service.PgTenancyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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

    @PostMapping
    public ResponseEntity<PgTenancy> createTenancy(@RequestBody PgTenancy tenancy) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tenancyService.createTenancy(tenancy));
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

    @PostMapping("/{id}/vacate")
    public ResponseEntity<PgTenancy> vacate(@PathVariable UUID id) {
        return ResponseEntity.ok(tenancyService.vacate(id));
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
}
