package com.safar.listing.controller;

import com.safar.listing.dto.*;
import com.safar.listing.service.AgreementService;
import jakarta.validation.Valid;
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
@RequestMapping("/api/v1/agreements")
@RequiredArgsConstructor
public class AgreementController {

    private final AgreementService agreementService;

    // ── Create ───────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<AgreementResponse> create(
            @Valid @RequestBody CreateAgreementRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(agreementService.create(request, userId));
    }

    // ── Get by ID ────────────────────────────────────────────

    @GetMapping("/{id}")
    public ResponseEntity<AgreementResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(agreementService.getById(id));
    }

    // ── My Agreements ────────────────────────────────────────

    @GetMapping("/my")
    public ResponseEntity<Page<AgreementResponse>> getMyAgreements(
            @RequestHeader("X-User-Id") UUID userId,
            Pageable pageable) {
        return ResponseEntity.ok(agreementService.getMyAgreements(userId, pageable));
    }

    // ── Update Draft ─────────────────────────────────────────

    @PutMapping("/{id}")
    public ResponseEntity<AgreementResponse> updateDraft(
            @PathVariable UUID id,
            @Valid @RequestBody CreateAgreementRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(agreementService.updateDraft(id, request, userId));
    }

    // ── Add Party ────────────────────────────────────────────

    @PostMapping("/{id}/parties")
    public ResponseEntity<AgreementPartyResponse> addParty(
            @PathVariable UUID id,
            @Valid @RequestBody AgreementPartyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(agreementService.addParty(id, request));
    }

    // ── Generate Draft ───────────────────────────────────────

    @PostMapping("/{id}/generate-draft")
    public ResponseEntity<AgreementResponse> generateDraft(@PathVariable UUID id) {
        return ResponseEntity.ok(agreementService.generateDraft(id));
    }

    // ── Process Payment ──────────────────────────────────────

    @PostMapping("/{id}/pay")
    public ResponseEntity<AgreementResponse> processPayment(
            @PathVariable UUID id,
            @RequestParam UUID paymentId) {
        return ResponseEntity.ok(agreementService.processPayment(id, paymentId));
    }

    // ── Init E-Sign ──────────────────────────────────────────

    @PostMapping("/{id}/sign/{partyId}")
    public ResponseEntity<AgreementPartyResponse> initESign(
            @PathVariable UUID id,
            @PathVariable UUID partyId) {
        return ResponseEntity.ok(agreementService.initESign(id, partyId));
    }

    // ── Admin: List All ────────────────────────────────────────

    @GetMapping("/admin/list")
    public ResponseEntity<Page<java.util.Map<String, Object>>> adminListAll(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            Pageable pageable) {
        if (!"ADMIN".equalsIgnoreCase(role)) throw new org.springframework.security.access.AccessDeniedException("Admin access required");
        return ResponseEntity.ok(agreementService.adminListAll(status, type, pageable));
    }

    // ── Admin: Update Status ─────────────────────────────────

    @PatchMapping("/{id}/status")
    public ResponseEntity<AgreementResponse> adminUpdateStatus(
            @PathVariable UUID id,
            @RequestParam String status,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        if (!"ADMIN".equalsIgnoreCase(role)) throw new org.springframework.security.access.AccessDeniedException("Admin access required");
        return ResponseEntity.ok(agreementService.adminUpdateStatus(id, status));
    }

    // ── Calculate Stamp Duty ─────────────────────────────────

    @GetMapping("/stamp-duty/calculate")
    public ResponseEntity<StampDutyCalculation> calculateStampDuty(
            @RequestParam String state,
            @RequestParam String agreementType,
            @RequestParam Long propertyValuePaise) {
        return ResponseEntity.ok(agreementService.calculateStampDuty(state, agreementType, propertyValuePaise));
    }

    // ── List Templates ───────────────────────────────────────

    @GetMapping("/templates")
    public ResponseEntity<List<Map<String, String>>> listTemplates() {
        List<Map<String, String>> templates = List.of(
                Map.of("type", "SALE_AGREEMENT", "description", "Agreement to sell immovable property"),
                Map.of("type", "SALE_DEED", "description", "Final sale deed for property transfer"),
                Map.of("type", "RENTAL_AGREEMENT", "description", "Rental agreement for residential/commercial property"),
                Map.of("type", "LEAVE_LICENSE", "description", "Leave and license agreement (Maharashtra style)"),
                Map.of("type", "PG_AGREEMENT", "description", "Paying guest accommodation agreement")
        );
        return ResponseEntity.ok(templates);
    }
}
