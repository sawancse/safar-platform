package com.safar.listing.service;

import com.safar.listing.dto.GuaranteeRequest;
import com.safar.listing.entity.GuaranteedIncomeContract;
import com.safar.listing.repository.GuaranteedIncomeContractRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuaranteedIncomeService {

    static final long MAX_GUARANTEE_PAISE = 5_000_000L;
    static final int MAX_DURATION_MONTHS = 12;

    private final GuaranteedIncomeContractRepository contractRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Transactional
    public GuaranteedIncomeContract createContract(UUID hostId, UUID listingId, GuaranteeRequest req) {
        long guaranteePaise = Math.min(req.monthlyGuaranteePaise(), MAX_GUARANTEE_PAISE);
        int durationMonths = Math.min(req.durationMonths(), MAX_DURATION_MONTHS);

        GuaranteedIncomeContract contract = GuaranteedIncomeContract.builder()
                .listingId(listingId)
                .hostId(hostId)
                .monthlyGuaranteePaise(guaranteePaise)
                .contractStart(req.contractStart())
                .contractEnd(req.contractStart().plusMonths(durationMonths))
                .status("ACTIVE")
                .totalPaidOutPaise(0L)
                .build();

        GuaranteedIncomeContract saved = contractRepository.save(contract);

        String payload = String.format(
                "{\"contractId\":\"%s\",\"listingId\":\"%s\",\"hostId\":\"%s\",\"monthlyGuaranteePaise\":%d}",
                saved.getId(), saved.getListingId(), saved.getHostId(), saved.getMonthlyGuaranteePaise());
        kafkaTemplate.send("guarantee.contract.created", saved.getId().toString(), payload);
        log.info("Guaranteed-income contract {} created for listing {}", saved.getId(), listingId);

        return saved;
    }

    public List<GuaranteedIncomeContract> getContracts(UUID hostId) {
        return contractRepository.findByHostId(hostId);
    }
}
