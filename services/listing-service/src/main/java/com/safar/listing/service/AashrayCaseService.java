package com.safar.listing.service;

import com.safar.listing.entity.AashrayCase;
import com.safar.listing.entity.enums.CasePriority;
import com.safar.listing.entity.enums.CaseStatus;
import com.safar.listing.repository.AashrayCaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AashrayCaseService {

    private final AashrayCaseRepository caseRepository;
    private final AashrayMatchingService matchingService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static long caseCounter = 1000;

    @Transactional
    public AashrayCase createCase(AashrayCase aashrayCase) {
        aashrayCase.setCaseNumber("ASH-2026-" + String.format("%04d", ++caseCounter));
        aashrayCase.setStatus(CaseStatus.OPEN);

        // Auto-escalate if children or elderly present
        if (aashrayCase.getChildren() > 0 || aashrayCase.getElderly() > 0) {
            if (aashrayCase.getPriority() == CasePriority.LOW || aashrayCase.getPriority() == CasePriority.MEDIUM) {
                aashrayCase.setPriority(CasePriority.HIGH);
            }
        }

        AashrayCase saved = caseRepository.save(aashrayCase);
        kafkaTemplate.send("aashray.case.created", saved.getId().toString(), saved);
        log.info("Aashray case created: {}", saved.getCaseNumber());
        return saved;
    }

    public Page<AashrayCase> getCases(CaseStatus status, String city, Pageable pageable) {
        if (status != null && city != null) {
            return caseRepository.findByStatusAndPreferredCity(status, city, pageable);
        } else if (status != null) {
            return caseRepository.findByStatus(status, pageable);
        } else if (city != null) {
            return caseRepository.findByPreferredCity(city, pageable);
        }
        return caseRepository.findAll(pageable);
    }

    public AashrayCase getCase(UUID id) {
        return caseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Aashray case not found: " + id));
    }

    @Transactional
    public AashrayCase updateCase(UUID id, AashrayCase update) {
        AashrayCase existing = getCase(id);
        if (update.getSeekerName() != null) existing.setSeekerName(update.getSeekerName());
        if (update.getSeekerPhone() != null) existing.setSeekerPhone(update.getSeekerPhone());
        if (update.getSeekerEmail() != null) existing.setSeekerEmail(update.getSeekerEmail());
        if (update.getFamilySize() > 0) existing.setFamilySize(update.getFamilySize());
        if (update.getPreferredCity() != null) existing.setPreferredCity(update.getPreferredCity());
        if (update.getPreferredLocality() != null) existing.setPreferredLocality(update.getPreferredLocality());
        if (update.getBudgetMaxPaise() > 0) existing.setBudgetMaxPaise(update.getBudgetMaxPaise());
        if (update.getLanguagesSpoken() != null) existing.setLanguagesSpoken(update.getLanguagesSpoken());
        if (update.getSpecialNeeds() != null) existing.setSpecialNeeds(update.getSpecialNeeds());
        if (update.getNotes() != null) existing.setNotes(update.getNotes());
        if (update.getNeedByDate() != null) existing.setNeedByDate(update.getNeedByDate());
        return caseRepository.save(existing);
    }

    @Transactional
    public AashrayCase updateStatus(UUID id, CaseStatus newStatus) {
        AashrayCase aashrayCase = getCase(id);
        aashrayCase.setStatus(newStatus);
        AashrayCase saved = caseRepository.save(aashrayCase);

        if (newStatus == CaseStatus.HOUSED) {
            kafkaTemplate.send("aashray.case.housed", saved.getId().toString(), saved);
        }
        return saved;
    }

    @Transactional
    public AashrayCase assignToListing(UUID caseId, UUID listingId) {
        AashrayCase aashrayCase = getCase(caseId);
        aashrayCase.setMatchedListingId(listingId);
        aashrayCase.setStatus(CaseStatus.MATCHED);
        AashrayCase saved = caseRepository.save(aashrayCase);
        kafkaTemplate.send("aashray.case.matched", saved.getId().toString(), saved);
        log.info("Case {} matched to listing {}", saved.getCaseNumber(), listingId);
        return saved;
    }

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalOpen", caseRepository.countByStatus(CaseStatus.OPEN));
        stats.put("totalMatched", caseRepository.countByStatus(CaseStatus.MATCHED));
        stats.put("totalHoused", caseRepository.countByStatus(CaseStatus.HOUSED));
        stats.put("totalClosed", caseRepository.countByStatus(CaseStatus.CLOSED));
        stats.put("byCity", caseRepository.getCaseStatsByCity());
        return stats;
    }
}
