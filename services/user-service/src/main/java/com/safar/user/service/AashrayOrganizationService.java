package com.safar.user.service;

import com.safar.user.dto.CaseWorkerDto;
import com.safar.user.dto.CreateOrganizationRequest;
import com.safar.user.dto.OrganizationDto;
import com.safar.user.entity.CaseWorker;
import com.safar.user.entity.Organization;
import com.safar.user.repository.CaseWorkerRepository;
import com.safar.user.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AashrayOrganizationService {

    private final OrganizationRepository organizationRepo;
    private final CaseWorkerRepository caseWorkerRepo;

    @Transactional
    public OrganizationDto createOrganization(CreateOrganizationRequest req) {
        Organization org = Organization.builder()
                .name(req.name())
                .type(req.type() != null ? req.type() : "NGO")
                .unhcrPartnerCode(req.unhcrPartnerCode())
                .contactEmail(req.contactEmail())
                .contactPhone(req.contactPhone())
                .budgetPaise(req.budgetPaise() != null ? req.budgetPaise() : 0L)
                .build();

        return toDto(organizationRepo.save(org));
    }

    public OrganizationDto getOrganization(UUID orgId) {
        Organization org = organizationRepo.findById(orgId)
                .orElseThrow(() -> new NoSuchElementException("Organization not found: " + orgId));
        return toDto(org);
    }

    public Page<OrganizationDto> listOrganizations(Pageable pageable) {
        return organizationRepo.findAll(pageable)
                .map(this::toDto);
    }

    @Transactional
    public CaseWorkerDto addCaseWorker(UUID orgId, UUID userId) {
        organizationRepo.findById(orgId)
                .orElseThrow(() -> new NoSuchElementException("Organization not found: " + orgId));

        CaseWorker cw = CaseWorker.builder()
                .userId(userId)
                .organizationId(orgId)
                .build();

        return toCaseWorkerDto(caseWorkerRepo.save(cw));
    }

    @Transactional
    public void removeCaseWorker(UUID caseWorkerId) {
        CaseWorker cw = caseWorkerRepo.findById(caseWorkerId)
                .orElseThrow(() -> new NoSuchElementException("Case worker not found: " + caseWorkerId));
        cw.setActive(false);
        caseWorkerRepo.save(cw);
    }

    public List<CaseWorkerDto> getCaseWorkers(UUID orgId) {
        return caseWorkerRepo.findByOrganizationId(orgId).stream()
                .map(this::toCaseWorkerDto)
                .toList();
    }

    @Transactional
    public OrganizationDto deductBudget(UUID orgId, long amountPaise) {
        Organization org = organizationRepo.findById(orgId)
                .orElseThrow(() -> new NoSuchElementException("Organization not found: " + orgId));

        long available = org.getBudgetPaise() - org.getSpentPaise();
        if (amountPaise > available) {
            throw new IllegalStateException("Insufficient budget. Available: " + available
                    + " paise, requested: " + amountPaise + " paise");
        }

        org.setSpentPaise(org.getSpentPaise() + amountPaise);
        return toDto(organizationRepo.save(org));
    }

    @Transactional
    public OrganizationDto addBudget(UUID orgId, long amountPaise) {
        Organization org = organizationRepo.findById(orgId)
                .orElseThrow(() -> new NoSuchElementException("Organization not found: " + orgId));

        org.setBudgetPaise(org.getBudgetPaise() + amountPaise);
        return toDto(organizationRepo.save(org));
    }

    private OrganizationDto toDto(Organization org) {
        return new OrganizationDto(
                org.getId(),
                org.getName(),
                org.getType(),
                org.getUnhcrPartnerCode(),
                org.getContactEmail(),
                org.getContactPhone(),
                org.getBudgetPaise(),
                org.getSpentPaise(),
                org.getBudgetPaise() - org.getSpentPaise(),
                org.getActive(),
                org.getCreatedAt(),
                org.getUpdatedAt()
        );
    }

    private CaseWorkerDto toCaseWorkerDto(CaseWorker cw) {
        return new CaseWorkerDto(
                cw.getId(),
                cw.getUserId(),
                cw.getOrganizationId(),
                cw.getRole(),
                cw.getActive()
        );
    }
}
