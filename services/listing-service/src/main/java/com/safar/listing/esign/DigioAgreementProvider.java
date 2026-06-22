package com.safar.listing.esign;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Digio aggregator adapter — Aadhaar eSign + e-Stamp behind one API.
 *
 * SCAFFOLD ONLY: the HTTP call sites are marked with TODO and currently throw,
 * so it never silently "succeeds" without a real integration. Enable by setting
 * {@code agreement.esign.provider=digio} once sandbox/prod credentials exist.
 *
 * Digio API reference (fill in at the TODO blocks):
 *  - Auth: HTTP Basic with client_id:client_secret (sandbox: https://ext.digio.in:444 ; prod: https://api.digio.in).
 *  - Create eSign: POST /v2/client/document/uploadpdf  (or /v3/.../template) with the PDF (base64) + signer
 *    list (name/email/phone, sign_type=aadhaar, coordinates). Response gives a document id + per-signer
 *    authentication/sign URLs.
 *  - Status: GET /v2/client/document/{id}.
 *  - Download signed: GET /v2/client/document/{id}/download.
 *  - e-Stamp: POST Digio eStamp API (state, doc_type, consideration, first/second party) -> certificate no + PDF.
 *  - Webhook: Digio posts signing events; verify the X-Digio-Signature HMAC over the raw body with the webhook secret.
 */
@Component
@Slf4j
public class DigioAgreementProvider implements EsignProvider, EStampProvider {

    @Value("${agreement.digio.base-url:https://ext.digio.in:444}")
    private String baseUrl;
    @Value("${agreement.digio.client-id:}")
    private String clientId;
    @Value("${agreement.digio.client-secret:}")
    private String clientSecret;
    @Value("${agreement.digio.webhook-secret:}")
    private String webhookSecret;

    @Override
    public String name() { return "DIGIO"; }

    @Override
    public EsignModels.EsignEnvelope createEnvelope(EsignModels.EsignCreateRequest req) {
        // TODO(digio): POST {baseUrl}/v2/client/document/uploadpdf with Basic(clientId:clientSecret),
        // body { file_data: base64(req.pdf), signers: [{ identifier: email/phone, name, sign_type: "aadhaar",
        // reason }], expire_in_days, display_on_page }. Map response.id -> documentId and the per-signer
        // authentication_url -> SignerLink(signingUrl).
        throw new UnsupportedOperationException("Digio eSign not yet wired — set agreement.esign.provider=sandbox or implement the TODO");
    }

    @Override
    public EsignModels.EsignStatusResult getStatus(String documentId) {
        // TODO(digio): GET {baseUrl}/v2/client/document/{documentId} -> map signing_parties[] to SignerStatus.
        throw new UnsupportedOperationException("Digio eSign status not yet wired");
    }

    @Override
    public byte[] downloadSignedPdf(String documentId) {
        // TODO(digio): GET {baseUrl}/v2/client/document/{documentId}/download -> signed PKCS#7 PDF bytes.
        throw new UnsupportedOperationException("Digio signed-PDF download not yet wired");
    }

    @Override
    public boolean verifyWebhook(String payload, String signature) {
        // TODO(digio): HMAC-SHA256(payload, webhookSecret) and constant-time compare to signature header.
        return false;
    }

    @Override
    public EsignModels.EsignWebhookEvent parseWebhook(String payload) {
        // TODO(digio): parse Digio webhook JSON (event=document.signed / completed) -> EsignWebhookEvent.
        throw new UnsupportedOperationException("Digio webhook parse not yet wired");
    }

    @Override
    public EsignModels.EStampResult issueStamp(EsignModels.EStampRequest req) {
        // TODO(digio): POST Digio eStamp endpoint (state, doc_type=req.agreementType, consideration_amount,
        // stamp_duty, first_party, second_party) -> certificate_number + stamp PDF url.
        throw new UnsupportedOperationException("Digio eStamp not yet wired — set agreement.estamp.provider=sandbox or implement the TODO");
    }
}
