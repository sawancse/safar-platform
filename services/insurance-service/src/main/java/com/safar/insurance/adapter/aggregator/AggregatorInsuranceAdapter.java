package com.safar.insurance.adapter.aggregator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.safar.insurance.adapter.*;
import com.safar.insurance.entity.enums.CoverageType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * PhonePe-style multi-insurer AGGREGATOR adapter (Riskcovry / Zopper / PB Partners class).
 *
 * Unlike a single-insurer adapter (Acko), an aggregator exposes MANY insurers' products
 * behind one REST contract; we are a distribution partner (corporate agent / broker) and
 * earn commission. A quote returns several plans across insurers — we surface the best-value
 * plan per our QuoteResult model and lock its {quoteId, planId} into the opaque quote token
 * so issue() binds the exact plan/premium the customer saw.
 *
 * ───────────────────────────────────────────────────────────────────────────────
 * GO-LIVE (creds arriving soon — "will partner soon"):
 *   1. Sign the aggregator partnership (Riskcovry/Zopper/PB Partners) → get
 *      partner_id + API key (sandbox + live).
 *   2. Set env: AGGREGATOR_ENABLED=true, AGGREGATOR_API_KEY, AGGREGATOR_PARTNER_ID,
 *      AGGREGATOR_BASE_URL, and INSURANCE_PRIMARY_PROVIDER=AGGREGATOR.
 *   3. Confirm the 3 endpoint paths + JSON field names below against the partner's
 *      API docs (they're modelled on the common partner-API shape; tweak the
 *      jsonPath strings / request bodies if the partner differs).
 *   4. Sandbox-test 20+ end-to-end issuances + 1 cancel/refund before flipping live.
 * The wire calls are REAL — only the credentials gate (isEnabled) keeps it dormant.
 * ───────────────────────────────────────────────────────────────────────────────
 */
@Component
@Slf4j
public class AggregatorInsuranceAdapter implements InsuranceProviderAdapter {

    private static final String TOKEN_SEP = "|";

    private final WebClient aggregatorWebClient;
    private final ObjectMapper objectMapper;

    @Value("${aggregator.enabled:false}")
    private boolean enabled;

    @Value("${aggregator.api-key:}")
    private String apiKey;

    @Value("${aggregator.partner-id:}")
    private String partnerId;

    @Value("${aggregator.timeout-ms:8000}")
    private long timeoutMs;

    public AggregatorInsuranceAdapter(@Qualifier("aggregatorWebClient") WebClient aggregatorWebClient,
                                      ObjectMapper objectMapper) {
        this.aggregatorWebClient = aggregatorWebClient;
        this.objectMapper = objectMapper;
    }

    @Override public InsuranceProvider providerType() { return InsuranceProvider.AGGREGATOR; }

    /** Dormant until the partnership is signed and creds are set. */
    @Override
    public boolean isEnabled() {
        return enabled
                && apiKey != null && !apiKey.isBlank()
                && partnerId != null && !partnerId.isBlank();
    }

    /** Build the /v1/quotes request body shared by quote() + comparePlans(). */
    private ObjectNode quoteBody(QuoteRequest req) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("product", productCode(req.coverageType()));
        body.put("coverage_type", req.coverageType().name().toLowerCase());
        body.put("start_date", String.valueOf(req.tripStartDate()));
        body.put("end_date", String.valueOf(req.tripEndDate()));
        ObjectNode geo = body.putObject("geography");
        geo.put("origin", nullSafe(req.tripOriginCode()));
        geo.put("destination", nullSafe(req.tripDestinationCode()));
        geo.put("origin_country", req.tripOriginCountry() != null ? req.tripOriginCountry() : "IN");
        geo.put("destination_country", req.tripDestinationCountry() != null ? req.tripDestinationCountry() : "IN");
        ArrayNode members = body.putArray("members");
        List<Integer> ages = req.travellerAges() != null && !req.travellerAges().isEmpty()
                ? req.travellerAges() : List.of(30);
        for (Integer age : ages) members.addObject().put("age", age != null ? age : 30);
        return body;
    }

    // ── Quote: fan-in across insurers, return the best-value plan ───────────────
    @Override
    public QuoteResult quote(QuoteRequest req) {
        try {
            JsonNode resp = post("/v1/quotes", quoteBody(req));
            String quoteId = resp.path("quote_id").asText("");
            JsonNode plans = resp.path("plans");
            if (!plans.isArray() || plans.isEmpty()) {
                throw new InsuranceProviderException("Aggregator returned no plans for " + req.coverageType());
            }

            // Best value = lowest premium (sort happens upstream too, but be defensive).
            JsonNode best = null;
            for (JsonNode plan : plans) {
                if (best == null || plan.path("premium").asLong(Long.MAX_VALUE)
                        < best.path("premium").asLong(Long.MAX_VALUE)) {
                    best = plan;
                }
            }

            String planId = best.path("plan_id").asText("");
            long premiumPaise = toPaise(best.path("premium").asDouble(0));
            long sumInsuredPaise = toPaise(best.path("sum_insured").asDouble(0));
            String currency = best.path("currency").asText("INR");
            String wordingUrl = best.path("wording_url").asText("");

            List<String> highlights = new ArrayList<>();
            JsonNode features = best.path("features");
            if (features.isArray()) features.forEach(f -> highlights.add(f.asText()));
            String insurer = best.path("insurer").asText("");
            if (!insurer.isBlank()) highlights.add(0, "Underwritten by " + insurer);

            log.info("[AGGREGATOR] quote {} -> {} plans, best='{}' premium ₹{}",
                    req.coverageType(), plans.size(), insurer, premiumPaise / 100);

            // token = quoteId|planId so issue() binds the exact plan the customer chose.
            return new QuoteResult(InsuranceProvider.AGGREGATOR,
                    quoteId + TOKEN_SEP + planId,
                    premiumPaise, sumInsuredPaise, currency, highlights, wordingUrl);
        } catch (InsuranceProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new InsuranceProviderException("Aggregator quote failed: " + e.getMessage(), e);
        }
    }

    // ── Compare: map ALL insurer plans (the PolicyBazaar grid) ──────────────────
    @Override
    public List<com.safar.insurance.dto.PlanOption> comparePlans(QuoteRequest req) {
        try {
            JsonNode resp = post("/v1/quotes", quoteBody(req));
            String quoteId = resp.path("quote_id").asText("");
            JsonNode plans = resp.path("plans");
            if (!plans.isArray() || plans.isEmpty()) {
                throw new InsuranceProviderException("Aggregator returned no plans for " + req.coverageType());
            }
            List<com.safar.insurance.dto.PlanOption> out = new ArrayList<>();
            for (JsonNode plan : plans) {
                String planId = plan.path("plan_id").asText("");
                List<String> features = new ArrayList<>();
                if (plan.path("features").isArray()) plan.path("features").forEach(f -> features.add(f.asText()));
                List<com.safar.insurance.dto.PlanOption.AddOn> addOns = new ArrayList<>();
                if (plan.path("add_ons").isArray()) {
                    for (JsonNode a : plan.path("add_ons")) {
                        addOns.add(new com.safar.insurance.dto.PlanOption.AddOn(
                                a.path("code").asText(""), a.path("label").asText(""),
                                toPaise(a.path("premium").asDouble(0))));
                    }
                }
                out.add(new com.safar.insurance.dto.PlanOption(
                        ProviderQuoteId.encode(InsuranceProvider.AGGREGATOR, quoteId + TOKEN_SEP + planId),
                        plan.path("insurer").asText(""),
                        plan.path("plan_name").asText(plan.path("insurer").asText("Plan")),
                        plan.path("tagline").asText(null),
                        toPaise(plan.path("premium").asDouble(0)),
                        toPaise(plan.path("sum_insured").asDouble(0)),
                        plan.path("currency").asText("INR"),
                        features,
                        plan.has("claim_settlement_ratio") ? plan.path("claim_settlement_ratio").asDouble() : null,
                        plan.has("cashless_count") ? plan.path("cashless_count").asInt() : null,
                        plan.has("insurer_rating") ? plan.path("insurer_rating").asDouble() : null,
                        addOns,
                        plan.path("wording_url").asText(""),
                        plan.path("recommended").asBoolean(false),
                        false));
            }
            log.info("[AGGREGATOR] compare {} -> {} plans", req.coverageType(), out.size());
            return out;
        } catch (InsuranceProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new InsuranceProviderException("Aggregator compare failed: " + e.getMessage(), e);
        }
    }

    // ── Issue: bind plan, create the policy, return the insurer's certificate ───
    @Override
    public IssueResult issue(IssueRequest req) {
        try {
            String[] parts = req.providerQuoteToken().split("\\" + TOKEN_SEP, 2);
            String quoteId = parts.length > 0 ? parts[0] : "";
            String planId = parts.length > 1 ? parts[1] : "";

            ObjectNode body = objectMapper.createObjectNode();
            body.put("quote_id", quoteId);
            body.put("plan_id", planId);
            ObjectNode proposer = body.putObject("proposer");
            String name = req.travellers() != null && !req.travellers().isEmpty()
                    ? (req.travellers().get(0).firstName() + " " + req.travellers().get(0).lastName()).trim()
                    : "Customer";
            proposer.put("name", name);
            proposer.put("email", nullSafe(req.contactEmail()));
            proposer.put("phone", nullSafe(req.contactPhone()));
            ArrayNode members = body.putArray("members");
            if (req.travellers() != null) {
                for (IssueRequest.Traveller t : req.travellers()) {
                    ObjectNode m = members.addObject();
                    m.put("first_name", t.firstName());
                    m.put("last_name", t.lastName());
                    m.put("dob", String.valueOf(t.dateOfBirth()));
                    m.put("gender", t.gender());
                    m.put("nationality", t.nationality());
                    if (t.passportNumber() != null) m.put("passport_number", t.passportNumber());
                    if (t.passportExpiry() != null) m.put("passport_expiry", String.valueOf(t.passportExpiry()));
                }
            }

            JsonNode resp = post("/v1/policies", body);
            String policyNumber = resp.path("policy_number").asText(resp.path("policy_id").asText(""));
            String certificateUrl = resp.path("certificate_url").asText("");
            long premiumPaise = toPaise(resp.path("premium").asDouble(0));
            long sumInsuredPaise = toPaise(resp.path("sum_insured").asDouble(0));
            String currency = resp.path("currency").asText("INR");
            String status = resp.path("status").asText("ISSUED");

            if (policyNumber.isBlank()) {
                throw new InsuranceProviderException("Aggregator issue returned no policy_number");
            }
            log.info("[AGGREGATOR] issued policy {} premium ₹{}", policyNumber, premiumPaise / 100);
            return new IssueResult(policyNumber, certificateUrl, premiumPaise, sumInsuredPaise, currency, status);
        } catch (InsuranceProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new InsuranceProviderException("Aggregator issue failed: " + e.getMessage(), e);
        }
    }

    // ── Cancel: provider decides refund per free-look / cancel grid ─────────────
    @Override
    public void cancel(String externalPolicyId) {
        if (externalPolicyId == null || externalPolicyId.isBlank()) return;
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("reason", "customer_request");
            JsonNode resp = post("/v1/policies/" + externalPolicyId + "/cancel", body);
            log.info("[AGGREGATOR] cancelled policy {} -> status={}, refund={}",
                    externalPolicyId, resp.path("status").asText(""), resp.path("refund_amount").asText("0"));
        } catch (Exception e) {
            throw new InsuranceProviderException("Aggregator cancel failed: " + e.getMessage(), e);
        }
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private JsonNode post(String path, JsonNode body) throws Exception {
        String raw = aggregatorWebClient.post()
                .uri(path)
                .header("Authorization", "Bearer " + apiKey)
                .header("X-Partner-Id", partnerId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(body))
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(timeoutMs))
                .block();
        return objectMapper.readTree(raw == null ? "{}" : raw);
    }

    /** Aggregator product code per coverage type (matches common partner taxonomy). */
    private static String productCode(CoverageType c) {
        return switch (c) {
            case INTERNATIONAL_TRAVEL, STUDENT_TRAVEL, DOMESTIC_TRAVEL -> "travel";
            case STAY_PROTECTION -> "stay_protection";
            case TENANT_CONTENTS, HOME -> "home";
            case HEALTH -> "health";
            case LIFE_TERM -> "term_life";
            case MOTOR -> "motor";
            case PERSONAL_ACCIDENT -> "personal_accident";
            default -> "general";
        };
    }

    /** Aggregator APIs quote in rupees (decimal); we store paise. */
    private static long toPaise(double rupees) { return Math.round(rupees * 100); }

    private static String nullSafe(String s) { return s == null ? "" : s; }
}
