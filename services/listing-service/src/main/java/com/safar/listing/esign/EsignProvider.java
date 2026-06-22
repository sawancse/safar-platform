package com.safar.listing.esign;

/**
 * Aadhaar eSign provider abstraction (eSign 2.1, IT Act §3A). An implementation
 * either talks to a licensed ESP/aggregator (e.g. Digio) or mocks the flow
 * (Sandbox). Mirrors the FlightProviderAdapter / InsuranceAdapter pattern.
 */
public interface EsignProvider {

    /** Provider id, e.g. "SANDBOX" or "DIGIO". Matched against {@code agreement.esign.provider}. */
    String name();

    /** Upload the (stamped) PDF + signers and create an eSign envelope; returns per-signer Aadhaar links. */
    EsignModels.EsignEnvelope createEnvelope(EsignModels.EsignCreateRequest req);

    /** Poll provider-side status of an envelope. */
    EsignModels.EsignStatusResult getStatus(String documentId);

    /** Fetch the final PKCS#7-signed PDF once all signers have completed. May return null if not ready. */
    byte[] downloadSignedPdf(String documentId);

    /** Verify a webhook callback's authenticity (HMAC signature). */
    boolean verifyWebhook(String payload, String signature);

    /** Parse a verified webhook payload into a normalized event. */
    EsignModels.EsignWebhookEvent parseWebhook(String payload);
}
