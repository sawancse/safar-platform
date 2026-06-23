package com.safar.insurance.controller;

import com.safar.insurance.entity.InsuranceLead;
import com.safar.insurance.service.InsuranceLeadService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/** Ops queue for advisor-callback leads. ADMIN-gated by SecurityConfig (/admin/**). */
@RestController
@RequestMapping("/api/v1/insurance/admin/leads")
@RequiredArgsConstructor
public class AdminInsuranceLeadController {

    private final InsuranceLeadService leadService;

    @GetMapping
    public ResponseEntity<Page<InsuranceLead>> list(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 30) Pageable pageable) {
        return ResponseEntity.ok(leadService.list(status, pageable));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<InsuranceLead> updateStatus(@PathVariable UUID id,
                                                      @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(leadService.updateStatus(id, body.getOrDefault("status", "CONTACTED")));
    }
}
