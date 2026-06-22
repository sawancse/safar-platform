# Agreement Aadhaar eSign + Government e-Stamp — Design & Implementation

Status: **Sandbox live + Digio integration wired (v2 API implemented).** 2026-06-22.
Digio activates when `agreement.esign.provider=digio` + credentials are set; two
fields need confirmation against your Digio account (signer gateway-URL format,
eStamp endpoint path) — see "To go live" below.

## Goal
Let parties execute rental/sale agreements with a legally-valid **Aadhaar eSign**
(IT Act §3A) and a **government e-Stamp** (state stamp duty) — replacing the
current click-to-sign + fictional `STAMPED` status flip.

## Legal / provider model
- **Aadhaar eSign (eSign 2.1):** the signer authenticates via Aadhaar OTP/biometric;
  a CCA-licensed **ESP** (Protean/NSDL, eMudhra, …) applies a PKCS#7 digital
  signature to the document hash. We integrate via an **aggregator** (chosen:
  **Digio**) acting as ASP, so we avoid direct CCA empanelment.
- **e-Stamp:** stamp duty paid to the state; **SHCIL** (central agency, ~22 states)
  or state portals issue an e-Stamp Certificate (UIN). Rate is **state × doc-type ×
  value** — already computed by `AgreementService.calculateStampDuty` →
  `stamp_duty_paise`. Digio also fronts e-Stamp issuance.
- **UIDAI compliance:** never store the raw Aadhaar number from the signing flow —
  only the **masked VID** + name the ESP returns (`agreement_parties.esign_signer_vid_masked`).

## Architecture (adapter pattern — mirrors flight/SCM/insurance)
`com.safar.listing.esign`:
- `EsignProvider` — `createEnvelope` / `getStatus` / `downloadSignedPdf` / `verifyWebhook` / `parseWebhook`.
- `EStampProvider` — `issueStamp`.
- `SandboxAgreementProvider` — **fully-mocked, default**; each signer gets a link to our
  own `/esign/sandbox-sign` endpoint that marks them signed. Lets the whole flow run with no creds.
- `DigioAgreementProvider` — **scaffold**; HTTP call sites are TODO blocks that throw (never
  silently succeeds). Enable with `agreement.esign.provider=digio` + creds.
- `AgreementProviderResolver` — selects provider by `agreement.esign.provider` / `agreement.estamp.provider`.
- `AgreementEsignService` — orchestration.

## Flow
1. `POST /api/v1/agreements/{id}/estamp` → compute duty → `issueStamp` → store
   `e_stamp_id` (cert no) + `estamp_provider/pdf_url/issued_at`, status `STAMPED`.
2. `POST /api/v1/agreements/{id}/esign` → render PDF (`AgreementPdfService.generateDraftPdf`)
   → `createEnvelope` with parties (name/email/phone/Aadhaar-name) → store
   `esign_document_id`, status `PENDING_SIGN`; each `agreement_parties` row gets
   `esign_signing_url` + `e_sign_request_id`. Returns the **Aadhaar signing links**.
3. Each party opens their link → Aadhaar OTP/biometric (Digio) or sandbox auto-complete.
4. Completion: Digio **webhook** `POST /api/v1/agreements/esign/webhook` (HMAC-verified) →
   `markPartySigned`; sandbox uses `GET /api/v1/agreements/esign/sandbox-sign`.
   When **all** parties signed → download signed PKCS#7 PDF → `signed_pdf_url`,
   `esign_status=SIGNED`, agreement status `SIGNED`.
5. `GET /api/v1/agreements/{id}/esign/status` → current state (public).

## Data model (V85, listing-service `listings` schema)
- `agreement_requests`: `esign_provider`, `esign_document_id`, `esign_status`,
  `unsigned_pdf_url`, `estamp_provider`, `estamp_pdf_url`, `estamp_issued_at`
  (reuses existing `e_stamp_id`, `signed_pdf_url`, `stamp_duty_paise`).
- `agreement_parties`: `esign_signing_url`, `esign_signer_vid_masked`
  (reuses existing `aadhaar_number`, `e_sign_status`, `e_sign_request_id`, `signed_at`).

## Config (`application.yml`)
```
agreement.esign.provider:  sandbox | digio   (env AGREEMENT_ESIGN_PROVIDER)
agreement.estamp.provider: sandbox | digio
agreement.digio.{base-url,client-id,client-secret,webhook-secret}
```

## To go live with Digio (remaining work)
1. Digio contract → sandbox then prod **client-id/secret + webhook secret**.
2. Fill the `DigioAgreementProvider` TODO blocks (uploadpdf + aadhaar signer list;
   status; download; eStamp endpoint; HMAC webhook verify + parse).
3. **Persist the signed PDF** to media/S3 in `AgreementEsignService.finalizeSigned`
   (currently falls back to the draft URL).
4. Set `agreement.esign.provider=digio`, `agreement.estamp.provider=digio`.
5. Wire Kafka `agreement.signed` emission (TODO in `finalizeSigned`).

## Not changed (separate vertical)
The PG `TenancyAgreement` (booking-service) keeps its two-party click-sign for now;
the same `EsignProvider`/`EStampProvider` interfaces can be reused there later
(it would need new per-party eSign columns — only `tenant_aadhaar_last4` exists today).
