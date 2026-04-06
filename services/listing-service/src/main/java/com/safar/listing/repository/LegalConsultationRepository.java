package com.safar.listing.repository;

import com.safar.listing.entity.LegalConsultation;
import com.safar.listing.entity.enums.ConsultationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LegalConsultationRepository extends JpaRepository<LegalConsultation, UUID> {

    List<LegalConsultation> findByLegalCaseId(UUID legalCaseId);

    List<LegalConsultation> findByAdvocateIdAndStatus(UUID advocateId, ConsultationStatus status);
}
