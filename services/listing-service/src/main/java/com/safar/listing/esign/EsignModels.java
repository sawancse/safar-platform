package com.safar.listing.esign;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** DTOs for the Aadhaar eSign + e-Stamp provider abstraction. */
public final class EsignModels {

    private EsignModels() {}

    // ── eSign ──────────────────────────────────────────────────────────────
    public record EsignSigner(String partyRef, String name, String email, String phone, String aadhaarName) {}

    public record EsignCreateRequest(UUID agreementId, String documentName, byte[] pdf,
                                     List<EsignSigner> signers, String reason) {}

    public record SignerLink(String partyRef, String signingUrl) {}

    /** Envelope created at the provider — carries per-signer Aadhaar signing links. */
    public record EsignEnvelope(String documentId, String status, List<SignerLink> signerLinks) {}

    public record SignerStatus(String partyRef, String status, String vidMasked, OffsetDateTime signedAt) {}

    public record EsignStatusResult(String documentId, String status, List<SignerStatus> signers) {}

    /** Normalized webhook event from a provider callback. */
    public record EsignWebhookEvent(String documentId, String partyRef, String vidMasked,
                                    boolean signed, boolean allSigned) {}

    // ── e-Stamp ────────────────────────────────────────────────────────────
    public record EStampRequest(String state, String agreementType, long considerationPaise,
                                long stampDutyPaise, String firstPartyName, String secondPartyName, String purpose) {}

    public record EStampResult(String certificateNo, String provider, String stampPdfUrl, OffsetDateTime issuedAt) {}
}
