package com.safar.listing.esign;

/**
 * Government e-Stamp provider abstraction. Issues a state stamp-duty certificate
 * (SHCIL / state portal, usually via the same aggregator as eSign).
 */
public interface EStampProvider {

    /** Provider id, e.g. "SANDBOX" or "DIGIO". Matched against {@code agreement.estamp.provider}. */
    String name();

    /** Purchase an e-stamp for the agreement; returns the certificate number + stamp PDF. */
    EsignModels.EStampResult issueStamp(EsignModels.EStampRequest req);
}
