package com.safar.listing.repository;

import com.safar.listing.entity.LegalVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LegalVerificationRepository extends JpaRepository<LegalVerification, UUID> {

    List<LegalVerification> findByLegalCaseId(UUID legalCaseId);
}
