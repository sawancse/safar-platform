package com.safar.listing.controller;

import com.safar.listing.entity.AgreementRequest;
import com.safar.listing.esign.EsignModels;
import com.safar.listing.service.AgreementEsignService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Aadhaar eSign + government e-Stamp endpoints for agreements.
 * Flow: POST /estamp -> POST /esign (returns Aadhaar signing links) ->
 * signer completes -> webhook (or sandbox-sign) finalizes -> GET /esign/status.
 */
@RestController
@RequestMapping("/api/v1/agreements")
@RequiredArgsConstructor
public class AgreementEsignController {

    private final AgreementEsignService esignService;

    /** Owner: purchase a government e-stamp for the agreement. */
    @PostMapping("/{id}/estamp")
    public ResponseEntity<Map<String, Object>> estamp(@PathVariable UUID id,
                                                      @RequestHeader("X-User-Id") UUID userId) {
        AgreementRequest a = esignService.issueEstamp(id, userId);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("status", a.getStatus());
        out.put("stampCertificateNumber", a.getStampCertificateNumber());
        out.put("estampProvider", a.getEstampProvider());
        out.put("estampPdfUrl", a.getEstampPdfUrl());
        out.put("stampDutyPaise", a.getStampDutyPaise());
        return ResponseEntity.ok(out);
    }

    /** Owner: create the eSign envelope; returns per-party Aadhaar signing links. */
    @PostMapping("/{id}/esign")
    public ResponseEntity<EsignModels.EsignEnvelope> initiateEsign(@PathVariable UUID id,
                                                                   @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(esignService.initiateEsign(id, userId));
    }

    /** Public status of the eSign/eStamp on an agreement. */
    @GetMapping("/{id}/esign/status")
    public ResponseEntity<Map<String, Object>> status(@PathVariable UUID id) {
        AgreementRequest a = esignService.getStatus(id);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("status", a.getStatus());
        out.put("esignStatus", a.getEsignStatus());
        out.put("esignProvider", a.getEsignProvider());
        out.put("esignDocumentId", a.getEsignDocumentId());
        out.put("stampCertificateNumber", a.getStampCertificateNumber());
        out.put("signedDocumentUrl", a.getSignedDocumentUrl());
        return ResponseEntity.ok(out);
    }

    /** Sandbox-only: a signer "completes" Aadhaar eSign (the SANDBOX provider points signing links here). */
    @GetMapping(value = "/esign/sandbox-sign", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> sandboxSign(@RequestParam("doc") String documentId,
                                              @RequestParam("party") String partyRef) {
        esignService.sandboxComplete(documentId, partyRef);
        return ResponseEntity.ok("<html><body style='font-family:sans-serif;text-align:center;padding:48px'>"
                + "<h2>✅ Signed (sandbox)</h2><p>This party has completed Aadhaar eSign in sandbox mode."
                + " You can close this window.</p></body></html>");
    }

    /** Stream the signed PDF (proxied from the eSign provider; draft fallback for sandbox). */
    @GetMapping(value = "/{id}/document/signed.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> signedPdf(@PathVariable UUID id) {
        byte[] pdf = esignService.downloadSignedPdf(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", "inline; filename=agreement-" + id + "-signed.pdf")
                .body(pdf);
    }

    /** Provider webhook (Digio etc.) — verified + parsed in the service. */
    @PostMapping("/esign/webhook")
    public ResponseEntity<Void> webhook(@RequestBody String payload,
                                        @RequestHeader(value = "X-Digio-Signature", required = false) String signature) {
        esignService.handleWebhook(payload, signature);
        return ResponseEntity.ok().build();
    }
}
