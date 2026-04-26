package com.safar.insurance.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safar.insurance.adapter.*;
import com.safar.insurance.dto.IssuePolicyRequest;
import com.safar.insurance.entity.InsurancePolicy;
import com.safar.insurance.entity.enums.PolicyStatus;
import com.safar.insurance.repository.InsurancePolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Owns InsurancePolicy persistence + lifecycle.
 *
 * issue() routes to the originating provider via the quoteId prefix
 * (PROVIDER:nativeToken). Persists the policy locally + publishes
 * insurance.policy.issued Kafka event for notification-service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InsurancePolicyService {

    private final InsuranceProviderRegistry registry;
    private final InsurancePolicyRepository policyRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public InsurancePolicy issue(UUID userId, IssuePolicyRequest request) {
        InsuranceProvider provider = ProviderQuoteId.provider(request.quoteId());
        String nativeToken = ProviderQuoteId.nativeToken(request.quoteId());
        InsuranceProviderAdapter adapter = registry.get(provider);

        IssueRequest providerReq = new IssueRequest(
                nativeToken,
                request.travellers().stream().map(t -> new IssueRequest.Traveller(
                        t.firstName(), t.lastName(), t.dateOfBirth(),
                        t.gender(), t.nationality(),
                        t.passportNumber(), t.passportExpiry()
                )).toList(),
                request.contactEmail(), request.contactPhone()
        );

        IssueResult result = adapter.issue(providerReq);

        InsurancePolicy policy;
        try {
            policy = InsurancePolicy.builder()
                    .userId(userId)
                    .policyRef(generatePolicyRef())
                    .provider(provider.name())
                    .externalPolicyId(result.externalPolicyId())
                    .status(PolicyStatus.PENDING_PAYMENT)
                    .coverageType(request.coverageType())
                    .tripOriginCode(request.tripOriginCode())
                    .tripDestinationCode(request.tripDestinationCode())
                    .tripOriginCountry(request.tripOriginCountry() != null ? request.tripOriginCountry() : "IN")
                    .tripDestinationCountry(request.tripDestinationCountry() != null ? request.tripDestinationCountry() : "IN")
                    .tripStartDate(request.tripStartDate())
                    .tripEndDate(request.tripEndDate())
                    .insuredCount(request.travellers().size())
                    .insuredJson(objectMapper.writeValueAsString(request.travellers()))
                    .contactEmail(request.contactEmail())
                    .contactPhone(request.contactPhone())
                    .premiumPaise(result.premiumPaise())
                    .sumInsuredPaise(result.sumInsuredPaise())
                    .currency(result.currency())
                    .certificateUrl(result.certificateUrl())
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize traveller payload", e);
        }

        policy = policyRepository.save(policy);
        log.info("Insurance policy issued: {} (provider={}, externalId={}, premium={} {})",
                policy.getPolicyRef(), provider, result.externalPolicyId(),
                result.premiumPaise(), result.currency());
        publishEvent("insurance.policy.issued", policy);
        return policy;
    }

    @Transactional
    public InsurancePolicy confirmPayment(UUID userId, UUID policyId,
                                          String razorpayOrderId, String razorpayPaymentId) {
        InsurancePolicy policy = requireOwned(userId, policyId);
        if (policy.getStatus() != PolicyStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("Policy is not in PENDING_PAYMENT status");
        }
        policy.setRazorpayOrderId(razorpayOrderId);
        policy.setRazorpayPaymentId(razorpayPaymentId);
        policy.setPaymentStatus("PAID");
        policy.setStatus(PolicyStatus.ISSUED);
        policy.setIssuedAt(Instant.now());
        return policyRepository.save(policy);
    }

    @Transactional
    public InsurancePolicy cancel(UUID userId, UUID policyId, String reason) {
        InsurancePolicy policy = requireOwned(userId, policyId);
        if (policy.getStatus() == PolicyStatus.CANCELLED || policy.getStatus() == PolicyStatus.REFUNDED) {
            throw new IllegalStateException("Policy is already cancelled/refunded");
        }
        try {
            registry.get(InsuranceProvider.valueOf(policy.getProvider())).cancel(policy.getExternalPolicyId());
        } catch (Exception e) {
            log.warn("Provider cancel failed for {}; proceeding with local cancel: {}",
                    policy.getPolicyRef(), e.getMessage());
        }
        policy.setStatus(PolicyStatus.CANCELLED);
        policy.setCancelledAt(Instant.now());
        policy.setCancellationReason(reason != null ? reason : "Cancelled by user");
        // Refund eligibility decided per provider's free-look + cancel grid;
        // for now we record the request, refund_amount comes from provider webhook later.
        publishEvent("insurance.policy.cancelled", policy);
        return policyRepository.save(policy);
    }

    public Page<InsurancePolicy> getMyPolicies(UUID userId, Pageable pageable) {
        return policyRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    public InsurancePolicy getPolicy(UUID policyId) {
        return policyRepository.findById(policyId)
                .orElseThrow(() -> new NoSuchElementException("Policy not found: " + policyId));
    }

    // ── helpers ─────────────────────────────────────────────────

    private InsurancePolicy requireOwned(UUID userId, UUID policyId) {
        InsurancePolicy p = getPolicy(policyId);
        if (!p.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Policy does not belong to this user");
        }
        return p;
    }

    private static String generatePolicyRef() {
        return "INS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /** Inferred age helper — useful for converting dob → age before quote. */
    public static int ageFromDob(LocalDate dob) {
        if (dob == null) return 30;
        return Period.between(dob, LocalDate.now()).getYears();
    }

    private void publishEvent(String topic, InsurancePolicy policy) {
        try {
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("policyId", policy.getId().toString());
            event.put("policyRef", policy.getPolicyRef());
            event.put("userId", policy.getUserId().toString());
            event.put("provider", policy.getProvider());
            event.put("status", policy.getStatus().name());
            event.put("coverageType", policy.getCoverageType().name());
            event.put("tripStartDate", policy.getTripStartDate().toString());
            event.put("tripEndDate", policy.getTripEndDate().toString());
            event.put("tripOriginCode", Optional.ofNullable(policy.getTripOriginCode()).orElse(""));
            event.put("tripDestinationCode", Optional.ofNullable(policy.getTripDestinationCode()).orElse(""));
            event.put("premiumPaise", policy.getPremiumPaise());
            event.put("currency", policy.getCurrency());
            event.put("insuredCount", policy.getInsuredCount());
            event.put("contactEmail", Optional.ofNullable(policy.getContactEmail()).orElse(""));
            event.put("contactPhone", Optional.ofNullable(policy.getContactPhone()).orElse(""));
            event.put("certificateUrl", Optional.ofNullable(policy.getCertificateUrl()).orElse(""));
            kafkaTemplate.send(topic, policy.getId().toString(), objectMapper.writeValueAsString(event));
        } catch (Exception e) {
            log.error("Failed to publish Kafka event {}: {}", topic, e.getMessage());
        }
    }
}
