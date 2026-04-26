package com.safar.insurance.adapter.icici;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safar.insurance.adapter.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * ICICI Lombard travel insurance — STUB / SCAFFOLD.
 *
 * Enterprise-grade IRDAI-licensed product. Onboarding 2-4 weeks, more
 * paperwork than Acko but maximum trust + financial stability. Use as
 * the "premium tier" alongside Acko's startup-friendly path.
 *
 * To make live:
 *   1. ICICI Lombard B2B partnership signed → API key + partner_code
 *   2. Set env: ICICI_LOMBARD_ENABLED=true, ICICI_LOMBARD_API_KEY,
 *      ICICI_LOMBARD_PARTNER_CODE
 *   3. Replace 3 TODO(icici-creds) blocks below with real WebClient calls
 *   4. Sandbox-test 20+ end-to-end issuances
 */
@Component
@Slf4j
public class ICICILombardInsuranceAdapter implements InsuranceProviderAdapter {

    private final WebClient iciciLombardWebClient;
    private final ObjectMapper objectMapper;

    @Value("${icici-lombard.enabled:false}")
    private boolean enabled;

    @Value("${icici-lombard.api-key:}")
    private String apiKey;

    @Value("${icici-lombard.partner-code:}")
    private String partnerCode;

    public ICICILombardInsuranceAdapter(@Qualifier("iciciLombardWebClient") WebClient iciciLombardWebClient,
                                         ObjectMapper objectMapper) {
        this.iciciLombardWebClient = iciciLombardWebClient;
        this.objectMapper = objectMapper;
    }

    @Override public InsuranceProvider providerType() { return InsuranceProvider.ICICI_LOMBARD; }

    @Override
    public boolean isEnabled() {
        return enabled
                && apiKey != null && !apiKey.isBlank()
                && partnerCode != null && !partnerCode.isBlank();
    }

    @Override
    public QuoteResult quote(QuoteRequest request) {
        // TODO(icici-creds): wire real call once partnership is signed.
        // POST /api/v1/travelinsurance/quote
        // Headers: Authorization: Bearer {apiKey}, X-Partner-Code: {partnerCode}
        // Body shape similar to Acko but with ICICI's specific field names —
        // travel_type ("DOM"/"INT"), no_of_pax, age_group, sum_insured_band, etc.
        log.warn("ICICILombardInsuranceAdapter.quote not implemented — sandbox creds pending");
        throw new InsuranceProviderException("ICICILombardInsuranceAdapter not yet live (sandbox creds pending)");
    }

    @Override
    public IssueResult issue(IssueRequest request) {
        // TODO(icici-creds): wire real call once partnership is signed.
        // POST /api/v1/travelinsurance/issue
        // Returns ICICI policy number + certificate URL + IRDAI-mandated
        // policy wording link.
        log.warn("ICICILombardInsuranceAdapter.issue not implemented — sandbox creds pending");
        throw new InsuranceProviderException("ICICILombardInsuranceAdapter not yet live (sandbox creds pending)");
    }

    @Override
    public void cancel(String externalPolicyId) {
        if (externalPolicyId == null || externalPolicyId.isBlank()) return;
        // TODO(icici-creds): wire real call once partnership is signed.
        log.warn("ICICILombardInsuranceAdapter.cancel not implemented — sandbox creds pending");
        throw new InsuranceProviderException("ICICILombardInsuranceAdapter not yet live (sandbox creds pending)");
    }
}
