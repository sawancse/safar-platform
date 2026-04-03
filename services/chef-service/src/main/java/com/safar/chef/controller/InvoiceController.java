package com.safar.chef.controller;

import com.safar.chef.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @GetMapping("/api/v1/chef-bookings/{id}/invoice")
    public ResponseEntity<Map<String, Object>> getBookingInvoice(@PathVariable UUID id) {
        return ResponseEntity.ok(invoiceService.generateBookingInvoice(id));
    }

    @GetMapping("/api/v1/chef-events/{id}/invoice")
    public ResponseEntity<Map<String, Object>> getEventInvoice(@PathVariable UUID id) {
        return ResponseEntity.ok(invoiceService.generateEventInvoice(id));
    }
}
