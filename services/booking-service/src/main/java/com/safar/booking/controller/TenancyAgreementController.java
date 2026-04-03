package com.safar.booking.controller;

import com.safar.booking.dto.AgreementResponse;
import com.safar.booking.dto.CreateAgreementRequest;
import com.safar.booking.entity.PgTenancy;
import com.safar.booking.entity.TenancyAgreement;
import com.safar.booking.service.AgreementPdfService;
import com.safar.booking.service.PgTenancyService;
import com.safar.booking.service.TenancyAgreementService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pg-tenancies/{tenancyId}/agreement")
@RequiredArgsConstructor
public class TenancyAgreementController {

    private final TenancyAgreementService agreementService;
    private final AgreementPdfService pdfService;
    private final PgTenancyService tenancyService;

    @PostMapping
    public ResponseEntity<AgreementResponse> createAgreement(
            @PathVariable UUID tenancyId,
            @RequestBody CreateAgreementRequest request) {
        TenancyAgreement agreement = agreementService.createAgreement(tenancyId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(agreementService.toResponse(agreement));
    }

    @GetMapping
    public ResponseEntity<AgreementResponse> getAgreement(@PathVariable UUID tenancyId) {
        TenancyAgreement agreement = agreementService.getByTenancyId(tenancyId);
        return ResponseEntity.ok(agreementService.toResponse(agreement));
    }

    @PostMapping("/host-sign")
    public ResponseEntity<AgreementResponse> hostSign(
            @PathVariable UUID tenancyId,
            HttpServletRequest request) {
        String ip = extractIp(request);
        TenancyAgreement agreement = agreementService.hostSign(tenancyId, ip);
        return ResponseEntity.ok(agreementService.toResponse(agreement));
    }

    @PostMapping("/tenant-sign")
    public ResponseEntity<AgreementResponse> tenantSign(
            @PathVariable UUID tenancyId,
            HttpServletRequest request) {
        String ip = extractIp(request);
        TenancyAgreement agreement = agreementService.tenantSign(tenancyId, ip);
        return ResponseEntity.ok(agreementService.toResponse(agreement));
    }

    @GetMapping("/text")
    public ResponseEntity<String> getAgreementText(@PathVariable UUID tenancyId) {
        TenancyAgreement agreement = agreementService.getByTenancyId(tenancyId);
        return ResponseEntity.ok(agreement.getAgreementText());
    }

    /**
     * View agreement as a styled HTML page (for browser viewing/printing).
     */
    @GetMapping(value = "/view", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> viewAgreementHtml(@PathVariable UUID tenancyId) {
        TenancyAgreement agreement = agreementService.getByTenancyId(tenancyId);
        PgTenancy tenancy = tenancyService.getTenancy(tenancyId);
        String html = pdfService.renderHtml(agreement, tenancy);
        return ResponseEntity.ok(html);
    }

    /**
     * Download agreement as PDF.
     */
    @GetMapping(value = "/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadPdf(@PathVariable UUID tenancyId) {
        TenancyAgreement agreement = agreementService.getByTenancyId(tenancyId);
        byte[] pdf = pdfService.generatePdf(agreement);

        String filename = "Safar-Agreement-" + agreement.getAgreementNumber() + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                .body(pdf);
    }

    /**
     * View agreement inline in browser (no download prompt).
     */
    @GetMapping(value = "/pdf/inline", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> viewPdfInline(@PathVariable UUID tenancyId) {
        TenancyAgreement agreement = agreementService.getByTenancyId(tenancyId);
        byte[] pdf = pdfService.generatePdf(agreement);

        String filename = "Safar-Agreement-" + agreement.getAgreementNumber() + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                .body(pdf);
    }

    private String extractIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
