package com.safar.listing.service;

import com.safar.listing.entity.ScoutLead;
import com.safar.listing.entity.enums.ScoutLeadStatus;
import com.safar.listing.repository.ScoutLeadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ScoutLeadService {

    private final ScoutLeadRepository scoutLeadRepository;

    @Transactional
    public ScoutLead createLead(String address, String city, Long estimatedIncomePaise) {
        ScoutLead lead = ScoutLead.builder()
                .address(address)
                .city(city)
                .estimatedIncomePaise(estimatedIncomePaise)
                .status(ScoutLeadStatus.PENDING)
                .build();
        return scoutLeadRepository.save(lead);
    }

    public Page<ScoutLead> getLeads(Pageable pageable) {
        return scoutLeadRepository.findAll(pageable);
    }

    @Transactional
    public ScoutLead updateStatus(UUID leadId, ScoutLeadStatus status) {
        ScoutLead lead = scoutLeadRepository.findById(leadId)
                .orElseThrow(() -> new NoSuchElementException("Scout lead not found: " + leadId));
        lead.setStatus(status);
        return scoutLeadRepository.save(lead);
    }
}
