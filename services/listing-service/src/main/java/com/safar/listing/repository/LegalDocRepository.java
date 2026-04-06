package com.safar.listing.repository;

import com.safar.listing.entity.LegalDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LegalDocRepository extends JpaRepository<LegalDocument, UUID> {

    List<LegalDocument> findByLegalCaseId(UUID legalCaseId);
}
