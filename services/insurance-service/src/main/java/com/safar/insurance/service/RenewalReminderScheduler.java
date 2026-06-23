package com.safar.insurance.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safar.insurance.entity.InsurancePolicy;
import com.safar.insurance.entity.enums.PolicyStatus;
import com.safar.insurance.repository.InsurancePolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Fires renewal reminders for policies expiring at T-30 and T-7 days
 * (publishes insurance.policy.renewal-due → notification-service emails the customer).
 * Daily at 09:00 IST.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RenewalReminderScheduler {

    private final InsurancePolicyRepository policyRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Kolkata")
    public void sendRenewalReminders() {
        LocalDate today = LocalDate.now();
        int[] windows = {30, 7};
        int total = 0;
        for (int days : windows) {
            LocalDate target = today.plusDays(days);
            for (InsurancePolicy p : policyRepository.findByStatusAndTripEndDate(PolicyStatus.ISSUED, target)) {
                publish(p, days);
                total++;
            }
        }
        if (total > 0) log.info("Insurance renewal reminders published: {}", total);
    }

    private void publish(InsurancePolicy p, int daysToExpiry) {
        try {
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("policyId", p.getId().toString());
            e.put("policyRef", p.getPolicyRef());
            e.put("userId", p.getUserId() != null ? p.getUserId().toString() : "");
            e.put("provider", p.getProvider());
            e.put("coverageType", p.getCoverageType().name());
            e.put("premiumPaise", p.getPremiumPaise());
            e.put("sumInsuredPaise", p.getSumInsuredPaise() != null ? p.getSumInsuredPaise() : 0);
            e.put("contactEmail", Optional.ofNullable(p.getContactEmail()).orElse(""));
            e.put("contactPhone", Optional.ofNullable(p.getContactPhone()).orElse(""));
            e.put("expiryDate", p.getTripEndDate().toString());
            e.put("daysToExpiry", daysToExpiry);
            kafkaTemplate.send("insurance.policy.renewal-due", p.getId().toString(), objectMapper.writeValueAsString(e));
        } catch (Exception ex) {
            log.error("Failed to publish renewal-due for {}: {}", p.getPolicyRef(), ex.getMessage());
        }
    }
}
