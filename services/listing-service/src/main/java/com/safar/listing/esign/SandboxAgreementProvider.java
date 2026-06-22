package com.safar.listing.esign;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Default dev/QA provider — mocks Aadhaar eSign + e-Stamp end-to-end with NO
 * external credentials. Each signer gets a "signing URL" pointing at our own
 * sandbox-sign endpoint (which marks the party signed). Lets the full flow
 * (estamp → esign → finalize) be exercised before a real Digio contract exists.
 */
@Component
@Slf4j
public class SandboxAgreementProvider implements EsignProvider, EStampProvider {

    /** Public base for sandbox signing links (gateway origin). Relative if blank. */
    @Value("${agreement.esign.public-base-url:}")
    private String baseUrl;

    @Override
    public String name() { return "SANDBOX"; }

    // ── eSign ──
    @Override
    public EsignModels.EsignEnvelope createEnvelope(EsignModels.EsignCreateRequest req) {
        String documentId = "SBX-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        List<EsignModels.SignerLink> links = req.signers().stream()
                .map(s -> new EsignModels.SignerLink(s.partyRef(),
                        baseUrl + "/api/v1/agreements/esign/sandbox-sign?doc=" + documentId + "&party=" + s.partyRef()))
                .collect(Collectors.toList());
        log.info("[SANDBOX eSign] envelope {} created for agreement {} with {} signer(s)",
                documentId, req.agreementId(), links.size());
        return new EsignModels.EsignEnvelope(documentId, "INITIATED", links);
    }

    @Override
    public EsignModels.EsignStatusResult getStatus(String documentId) {
        // Sandbox tracks signing in our DB (via sandbox-sign), so nothing provider-side.
        return new EsignModels.EsignStatusResult(documentId, "INITIATED", List.of());
    }

    @Override
    public byte[] downloadSignedPdf(String documentId) {
        // No real PKCS#7 signing in sandbox — the orchestrator reuses the unsigned PDF as the "signed" artifact.
        return null;
    }

    @Override
    public boolean verifyWebhook(String payload, String signature) { return true; }

    @Override
    public EsignModels.EsignWebhookEvent parseWebhook(String payload) {
        return new EsignModels.EsignWebhookEvent(null, null, null, false, false);
    }

    // ── e-Stamp ──
    @Override
    public EsignModels.EStampResult issueStamp(EsignModels.EStampRequest req) {
        String cert = "IN-SBX-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();
        log.info("[SANDBOX eStamp] issued certificate {} for {} ({}), duty ₹{}",
                cert, req.agreementType(), req.state(), req.stampDutyPaise() / 100);
        return new EsignModels.EStampResult(cert, "SANDBOX", null, OffsetDateTime.now());
    }
}
