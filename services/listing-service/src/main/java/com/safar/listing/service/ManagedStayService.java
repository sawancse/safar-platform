package com.safar.listing.service;

import com.safar.listing.dto.ManagedEnrollRequest;
import com.safar.listing.dto.ManagedExpenseRequest;
import com.safar.listing.entity.ManagedStayContract;
import com.safar.listing.entity.ManagedStayExpense;
import com.safar.listing.repository.ManagedStayContractRepository;
import com.safar.listing.repository.ManagedStayExpenseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManagedStayService {

    private final ManagedStayContractRepository contractRepository;
    private final ManagedStayExpenseRepository expenseRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Transactional
    public ManagedStayContract enrollListing(UUID hostId, UUID listingId, ManagedEnrollRequest req) {
        contractRepository.findByListingIdAndStatus(listingId, "ACTIVE")
                .ifPresent(c -> {
                    throw new IllegalStateException(
                            "Listing " + listingId + " already has an active managed-stay contract");
                });

        ManagedStayContract contract = ManagedStayContract.builder()
                .listingId(listingId)
                .hostId(hostId)
                .managementFeePct(req.managementFeePct() != null ? req.managementFeePct() : 18)
                .contractStart(req.contractStart())
                .autoPricing(req.autoPricing() != null ? req.autoPricing() : true)
                .autoCleaning(req.autoCleaning() != null ? req.autoCleaning() : true)
                .guestScreening(req.guestScreening() != null ? req.guestScreening() : true)
                .status("ACTIVE")
                .build();

        ManagedStayContract saved = contractRepository.save(contract);

        String payload = String.format(
                "{\"contractId\":\"%s\",\"listingId\":\"%s\",\"hostId\":\"%s\"}",
                saved.getId(), saved.getListingId(), saved.getHostId());
        kafkaTemplate.send("managed.listing.enrolled", saved.getId().toString(), payload);
        log.info("Managed-stay contract {} created for listing {}", saved.getId(), listingId);

        return saved;
    }

    @Transactional
    public ManagedStayContract terminateContract(UUID hostId, UUID contractId) {
        ManagedStayContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new NoSuchElementException("Contract not found: " + contractId));
        if (!contract.getHostId().equals(hostId)) {
            throw new IllegalArgumentException("Contract does not belong to this host");
        }
        contract.setStatus("TERMINATED");
        return contractRepository.save(contract);
    }

    @Transactional
    public ManagedStayExpense recordExpense(UUID contractId, ManagedExpenseRequest req) {
        contractRepository.findById(contractId)
                .orElseThrow(() -> new NoSuchElementException("Contract not found: " + contractId));

        ManagedStayExpense expense = ManagedStayExpense.builder()
                .contractId(contractId)
                .bookingId(req.bookingId())
                .expenseType(req.expenseType())
                .amountPaise(req.amountPaise())
                .description(req.description())
                .receiptUrl(req.receiptUrl())
                .build();

        return expenseRepository.save(expense);
    }

    public List<ManagedStayContract> getContracts(UUID hostId) {
        return contractRepository.findByHostId(hostId);
    }
}
