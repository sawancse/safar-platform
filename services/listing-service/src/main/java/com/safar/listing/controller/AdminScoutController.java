package com.safar.listing.controller;

import com.safar.listing.entity.ScoutLead;
import com.safar.listing.entity.enums.ScoutLeadStatus;
import com.safar.listing.service.ScoutLeadService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/scout")
@RequiredArgsConstructor
public class AdminScoutController {

    private final ScoutLeadService scoutLeadService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/leads")
    public ResponseEntity<Page<ScoutLead>> getLeads(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(scoutLeadService.getLeads(pageable));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/leads/{id}/status")
    public ResponseEntity<ScoutLead> updateStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        ScoutLeadStatus status = ScoutLeadStatus.valueOf(body.get("status"));
        return ResponseEntity.ok(scoutLeadService.updateStatus(id, status));
    }
}
