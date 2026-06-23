package com.safar.insurance.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safar.insurance.entity.InsuranceLead;
import com.safar.insurance.repository.InsuranceLeadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Owns advisor-callback leads + publishes insurance.lead.created for ops notification. */
@Service
@RequiredArgsConstructor
@Slf4j
public class InsuranceLeadService {

    private final InsuranceLeadRepository leadRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public InsuranceLead create(UUID userId, String name, String phone, String email,
                                String product, String coverageType, String city,
                                String preferredTime, String notes) {
        InsuranceLead lead = InsuranceLead.builder()
                .userId(userId)
                .name(name != null && !name.isBlank() ? name : "Customer")
                .phone(phone)
                .email(email)
                .product(product)
                .coverageType(coverageType)
                .city(city)
                .preferredTime(preferredTime)
                .notes(notes)
                .status("NEW")
                .source("WEB_ADVISOR")
                .build();
        lead = leadRepository.save(lead);
        log.info("Insurance advisor lead created: {} ({} / {})", lead.getId(), lead.getProduct(), lead.getPhone());
        publish(lead);
        return lead;
    }

    public Page<InsuranceLead> list(String status, Pageable pageable) {
        return (status == null || status.isBlank())
                ? leadRepository.findAllByOrderByCreatedAtDesc(pageable)
                : leadRepository.findByStatusOrderByCreatedAtDesc(status.toUpperCase(), pageable);
    }

    @Transactional
    public InsuranceLead updateStatus(UUID id, String status) {
        InsuranceLead lead = leadRepository.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException("Lead not found: " + id));
        lead.setStatus(status.toUpperCase());
        return leadRepository.save(lead);
    }

    private void publish(InsuranceLead lead) {
        try {
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("leadId", lead.getId().toString());
            e.put("name", lead.getName());
            e.put("phone", lead.getPhone());
            e.put("email", Optional.ofNullable(lead.getEmail()).orElse(""));
            e.put("product", Optional.ofNullable(lead.getProduct()).orElse(""));
            e.put("coverageType", Optional.ofNullable(lead.getCoverageType()).orElse(""));
            e.put("city", Optional.ofNullable(lead.getCity()).orElse(""));
            e.put("preferredTime", Optional.ofNullable(lead.getPreferredTime()).orElse(""));
            kafkaTemplate.send("insurance.lead.created", lead.getId().toString(), objectMapper.writeValueAsString(e));
        } catch (Exception ex) {
            log.error("Failed to publish insurance.lead.created: {}", ex.getMessage());
        }
    }
}
