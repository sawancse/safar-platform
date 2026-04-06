package com.safar.listing.repository;

import com.safar.listing.entity.AgreementParty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AgreementPartyRepository extends JpaRepository<AgreementParty, UUID> {

    List<AgreementParty> findByAgreementRequestId(UUID agreementRequestId);
}
