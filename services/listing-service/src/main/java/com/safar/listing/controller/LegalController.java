package com.safar.listing.controller;

import com.safar.listing.dto.*;
import com.safar.listing.entity.LegalDocument;
import com.safar.listing.service.LegalReportPdfService;
import com.safar.listing.service.LegalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/legal")
@RequiredArgsConstructor
public class LegalController {

    private final LegalService legalService;
    private final LegalReportPdfService legalReportPdfService;

    // ── Create Case ──────────────────────────────────────────

    @PostMapping("/cases")
    public ResponseEntity<LegalCaseResponse> createCase(
            @Valid @RequestBody CreateLegalCaseRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(legalService.createCase(request, userId));
    }

    // ── All Cases (Admin) ────────────────────────────────────

    @GetMapping("/cases")
    public ResponseEntity<Page<LegalCaseResponse>> getAllCases(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String packageType,
            @RequestParam(required = false) String riskLevel,
            Pageable pageable) {
        requireAdmin(role);
        return ResponseEntity.ok(legalService.getAllCases(status, packageType, riskLevel, pageable));
    }

    // ── My Cases ─────────────────────────────────────────────

    @GetMapping("/cases/my")
    public ResponseEntity<Page<LegalCaseResponse>> getMyCases(
            @RequestHeader("X-User-Id") UUID userId,
            Pageable pageable) {
        return ResponseEntity.ok(legalService.getMyCases(userId, pageable));
    }

    // ── Get Case ─────────────────────────────────────────────

    @GetMapping("/cases/{id}")
    public ResponseEntity<LegalCaseResponse> getCase(@PathVariable UUID id) {
        return ResponseEntity.ok(legalService.getCase(id));
    }

    // ── Upload Document ──────────────────────────────────────

    @PostMapping("/cases/{id}/documents")
    public ResponseEntity<Void> uploadDocument(
            @PathVariable UUID id,
            @Valid @RequestBody LegalDocumentRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        legalService.uploadDocument(id, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // ── Get Case Documents ───────────────────────────────────

    @GetMapping("/cases/{id}/documents")
    public ResponseEntity<List<LegalDocument>> getCaseDocuments(@PathVariable UUID id) {
        return ResponseEntity.ok(legalService.getCaseDocuments(id));
    }

    // ── Download Report ──────────────────────────────────────

    @GetMapping(value = {"/cases/{id}/report", "/cases/{id}/report.pdf"})
    public ResponseEntity<byte[]> downloadReport(@PathVariable UUID id) {
        byte[] pdf = legalService.generateReportPdf(id, legalReportPdfService);
        if (pdf == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"legal-report-" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
    }

    // ── Schedule Consultation ────────────────────────────────

    @PostMapping("/cases/{id}/consultation")
    public ResponseEntity<ConsultationResponse> scheduleConsultation(
            @PathVariable UUID id,
            @Valid @RequestBody ScheduleConsultationRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(legalService.scheduleConsultation(id, request, userId));
    }

    // ── List Packages ────────────────────────────────────────

    @GetMapping("/packages")
    public ResponseEntity<List<Map<String, Object>>> listPackages() {
        return ResponseEntity.ok(legalService.listPackages());
    }

    // Advocates CRUD moved to ProfessionalController

    // ── Update Case Status (Admin) ───────────────────────────

    @PatchMapping("/cases/{id}/status")
    public ResponseEntity<LegalCaseResponse> updateCaseStatus(
            @PathVariable UUID id,
            @RequestParam String status,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        requireAdmin(role);
        return ResponseEntity.ok(legalService.updateCaseStatus(id, status));
    }

    // ── Assign Advocate (Admin) ──────────────────────────────

    @PostMapping("/cases/{id}/assign")
    public ResponseEntity<LegalCaseResponse> assignAdvocate(
            @PathVariable UUID id,
            @RequestParam UUID advocateId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        requireAdmin(role);
        return ResponseEntity.ok(legalService.assignAdvocate(id, advocateId));
    }

    // ── Generate Report (Admin) ──────────────────────────────

    @PostMapping("/cases/{id}/generate-report")
    public ResponseEntity<LegalCaseResponse> generateReport(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        requireAdmin(role);
        return ResponseEntity.ok(legalService.generateReport(id));
    }

    private void requireAdmin(String role) {
        if (!"ADMIN".equalsIgnoreCase(role)) {
            throw new org.springframework.security.access.AccessDeniedException("Admin access required");
        }
    }
}
