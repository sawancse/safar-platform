package com.safar.insurance.adapter.acko;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safar.insurance.adapter.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

/**
 * Acko travel insurance — STUB / SCAFFOLD.
 *
 * Acko's B2B partner API is REST/JSON, startup-friendly. Onboarding
 * typically 1-2 weeks: sign up at https://acko.com/business → product
 * partnership → travel insurance. Sandbox + live keys issued together.
 *
 * To make live:
 *   1. Acko partnership signed → API key + partner_id issued
 *   2. Set env: ACKO_ENABLED=true, ACKO_API_KEY, ACKO_PARTNER_ID
 *   3. Replace 3 TODO(acko-creds) blocks below with real WebClient calls
 *      (~50 lines each)
 *   4. Sandbox-test 20+ end-to-end issuances before going live
 */
@Component
@Slf4j
public class AckoInsuranceAdapter implements InsuranceProviderAdapter {

    private final WebClient ackoWebClient;
    private final ObjectMapper objectMapper;

    @Value("${acko.enabled:false}")
    private boolean enabled;

    @Value("${acko.api-key:}")
    private String apiKey;

    @Value("${acko.partner-id:}")
    private String partnerId;

    public AckoInsuranceAdapter(@Qualifier("ackoWebClient") WebClient ackoWebClient,
                                ObjectMapper objectMapper) {
        this.ackoWebClient = ackoWebClient;
        this.objectMapper = objectMapper;
    }

    @Override public InsuranceProvider providerType() { return InsuranceProvider.ACKO; }

    @Override
    public boolean isEnabled() {
        return enabled
                && apiKey != null && !apiKey.isBlank()
                && partnerId != null && !partnerId.isBlank();
    }

    @Override
    public QuoteResult quote(QuoteRequest request) {
        // TODO(acko-creds): wire real call once sandbox is open.
        // POST /v1/insurance/travel/quote
        // Headers: X-Api-Key: {apiKey}, X-Partner-Id: {partnerId}
        // Body: {
        //   "origin": "DEL", "destination": "BOM",
        //   "start_date": "2026-05-15", "end_date": "2026-05-20",
        //   "coverage_type": "domestic_travel",  // or "international_travel"
        //   "travellers": [{"age": 30}, {"age": 28}, ...]
        // }
        // Response: { "quote_token": "...", "premium": 599, "sum_insured": 50000,
        //             "currency": "INR", "highlights": [...], "wording_url": "..." }
        log.warn("AckoInsuranceAdapter.quote not implemented — sandbox creds pending");
        throw new InsuranceProviderException("AckoInsuranceAdapter not yet live (sandbox creds pending)");
    }

    @Override
    public IssueResult issue(IssueRequest request) {
        // TODO(acko-creds): wire real call once sandbox is open.
        // POST /v1/insurance/travel/issue
        // Body: {
        //   "quote_token": request.providerQuoteToken,
        //   "travellers": [{firstName, lastName, dob, gender, nationality, passport_no, passport_expiry}],
        //   "contact": {email, phone}
        // }
        // Response: { "policy_id": "ACK-XXXX", "certificate_url": "https://...",
        //             "premium": 599, "sum_insured": 50000, "currency": "INR",
        //             "status": "ISSUED" }
        log.warn("AckoInsuranceAdapter.issue not implemented — sandbox creds pending");
        throw new InsuranceProviderException("AckoInsuranceAdapter not yet live (sandbox creds pending)");
    }

    @Override
    public void cancel(String externalPolicyId) {
        if (externalPolicyId == null || externalPolicyId.isBlank()) return;
        // TODO(acko-creds): wire real call once sandbox is open.
        // POST /v1/insurance/travel/{externalPolicyId}/cancel
        // Response includes refund_amount based on Acko's free-look + cancel grid.
        log.warn("AckoInsuranceAdapter.cancel not implemented — sandbox creds pending");
        throw new InsuranceProviderException("AckoInsuranceAdapter not yet live (sandbox creds pending)");
    }
}
