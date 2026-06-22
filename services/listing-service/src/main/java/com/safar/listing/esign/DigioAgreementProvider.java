package com.safar.listing.esign;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Digio aggregator adapter — Aadhaar eSign + e-Stamp (Digio v2 API).
 *
 * Implements the documented native endpoints. Two integration-specific bits are
 * CONFIGURABLE and should be confirmed against your Digio account before prod:
 *   - the signer gateway-URL format ({@code agreement.digio.gateway-url})
 *   - the e-Stamp endpoint path/fields ({@code agreement.digio.estamp-path}); Digio
 *     provisions eStamp per-merchant, so the exact contract varies.
 *
 * Auth: HTTP Basic client_id:client_secret. Base: test https://ext.digio.in:444,
 * prod https://api.digio.in.
 */
@Component
@Slf4j
public class DigioAgreementProvider implements EsignProvider, EStampProvider {

    private final RestTemplate http;
    private final ObjectMapper json;

    @Value("${agreement.digio.base-url:https://ext.digio.in:444}") private String baseUrl;
    @Value("${agreement.digio.gateway-url:https://ext.digio.in}")  private String gatewayUrl;
    @Value("${agreement.digio.client-id:}")                        private String clientId;
    @Value("${agreement.digio.client-secret:}")                    private String clientSecret;
    @Value("${agreement.digio.webhook-secret:}")                   private String webhookSecret;
    @Value("${agreement.digio.expire-in-days:15}")                 private int expireInDays;
    @Value("${agreement.digio.estamp-path:}")                      private String estampPath;

    public DigioAgreementProvider(RestTemplate http, ObjectMapper json) {
        this.http = http;
        this.json = json;
    }

    @Override public String name() { return "DIGIO"; }

    private HttpHeaders authHeaders() {
        HttpHeaders h = new HttpHeaders();
        String basic = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));
        h.set(HttpHeaders.AUTHORIZATION, "Basic " + basic);
        return h;
    }

    // ── eSign ────────────────────────────────────────────────────────────────
    @Override
    public EsignModels.EsignEnvelope createEnvelope(EsignModels.EsignCreateRequest req) {
        if (clientId.isBlank()) throw new IllegalStateException("Digio client-id not configured");
        try {
            // POST /v2/client/document/uploadpdf — multipart: file + request(JSON)
            ObjectNode request = json.createObjectNode();
            ArrayNode signers = request.putArray("signers");
            for (EsignModels.EsignSigner s : req.signers()) {
                ObjectNode sn = signers.addObject();
                sn.put("identifier", s.email() != null && !s.email().isBlank() ? s.email() : s.phone());
                sn.put("name", s.name());
                sn.put("reason", req.reason() != null ? req.reason() : "Agreement execution");
                sn.put("sign_type", "aadhaar");
            }
            request.put("expire_in_days", expireInDays);
            request.put("display_on_page", "all");
            request.put("notify_signers", true);
            request.put("send_sign_link", true);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("request", json.writeValueAsString(request));
            ByteArrayResource file = new ByteArrayResource(req.pdf()) {
                @Override public String getFilename() { return req.documentName() + ".pdf"; }
            };
            body.add("file", file);

            HttpHeaders headers = authHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            ResponseEntity<String> resp = http.exchange(
                    baseUrl + "/v2/client/document/uploadpdf", HttpMethod.POST,
                    new HttpEntity<>(body, headers), String.class);

            JsonNode root = json.readTree(resp.getBody());
            String documentId = root.path("id").asText();
            String status = root.path("agreement_status").asText("requested");
            List<EsignModels.SignerLink> links = new ArrayList<>();
            for (EsignModels.EsignSigner s : req.signers()) {
                String identifier = s.email() != null && !s.email().isBlank() ? s.email() : s.phone();
                // Digio Gateway signing URL (confirm exact format for your account):
                String url = gatewayUrl + "/#/gateway/login/" + documentId + "/" + identifier;
                links.add(new EsignModels.SignerLink(s.partyRef(), url));
            }
            log.info("[DIGIO] eSign envelope {} created ({} signers)", documentId, links.size());
            return new EsignModels.EsignEnvelope(documentId, status, links);
        } catch (Exception e) {
            throw new RuntimeException("Digio createEnvelope failed: " + e.getMessage(), e);
        }
    }

    @Override
    public EsignModels.EsignStatusResult getStatus(String documentId) {
        try {
            ResponseEntity<String> resp = http.exchange(
                    baseUrl + "/v2/client/document/" + documentId, HttpMethod.GET,
                    new HttpEntity<>(authHeaders()), String.class);
            JsonNode root = json.readTree(resp.getBody());
            List<EsignModels.SignerStatus> signers = new ArrayList<>();
            for (JsonNode p : root.path("signing_parties")) {
                signers.add(new EsignModels.SignerStatus(
                        p.path("identifier").asText(), p.path("status").asText(), null, null));
            }
            return new EsignModels.EsignStatusResult(documentId, root.path("agreement_status").asText(), signers);
        } catch (Exception e) {
            throw new RuntimeException("Digio getStatus failed: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] downloadSignedPdf(String documentId) {
        ResponseEntity<byte[]> resp = http.exchange(
                baseUrl + "/v2/client/document/" + documentId + "/download", HttpMethod.GET,
                new HttpEntity<>(authHeaders()), byte[].class);
        return resp.getBody();
    }

    @Override
    public boolean verifyWebhook(String payload, String signature) {
        // Digio doesn't HMAC-sign webhooks by default; gate on a shared secret token
        // (configured in the Digio dashboard webhook URL / header). If unset, accept.
        if (webhookSecret.isBlank()) return true;
        return webhookSecret.equals(signature);
    }

    @Override
    public EsignModels.EsignWebhookEvent parseWebhook(String payload) {
        try {
            JsonNode root = json.readTree(payload);
            JsonNode doc = root.has("payload") ? root.path("payload").path("document") : root.path("document");
            String documentId = doc.path("id").asText(null);
            String status = doc.path("agreement_status").asText("");
            boolean allSigned = "completed".equalsIgnoreCase(status) || "signed".equalsIgnoreCase(status);
            // Webhook fires after all signers act (Digio), so mark all signed; partyRef null = all.
            return new EsignModels.EsignWebhookEvent(documentId, null, null, allSigned, allSigned);
        } catch (Exception e) {
            throw new RuntimeException("Digio parseWebhook failed: " + e.getMessage(), e);
        }
    }

    // ── e-Stamp ──────────────────────────────────────────────────────────────
    @Override
    public EsignModels.EStampResult issueStamp(EsignModels.EStampRequest req) {
        if (estampPath.isBlank()) {
            throw new IllegalStateException("Digio eStamp endpoint not configured (set agreement.digio.estamp-path) — confirm contract with Digio");
        }
        try {
            ObjectNode b = json.createObjectNode();
            b.put("state", req.state());
            b.put("doc_type", req.agreementType());
            b.put("stamp_duty_amount", req.stampDutyPaise() / 100.0);
            b.put("consideration_amount", req.considerationPaise() / 100.0);
            b.put("first_party", req.firstPartyName());
            b.put("second_party", req.secondPartyName());
            b.put("purpose", req.purpose());
            HttpHeaders headers = authHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<String> resp = http.exchange(
                    baseUrl + estampPath, HttpMethod.POST,
                    new HttpEntity<>(json.writeValueAsString(b), headers), String.class);
            JsonNode root = json.readTree(resp.getBody());
            String cert = root.path("certificate_number").asText(root.path("e_stamp_id").asText(null));
            String pdf = root.path("stamp_pdf_url").asText(null);
            return new EsignModels.EStampResult(cert, "DIGIO", pdf, OffsetDateTime.now());
        } catch (Exception e) {
            throw new RuntimeException("Digio issueStamp failed: " + e.getMessage(), e);
        }
    }
}
