package com.safar.listing.repository;

import com.safar.listing.entity.AgreementRequest;
import com.safar.listing.entity.enums.AgreementStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AgreementRequestRepository extends JpaRepository<AgreementRequest, UUID> {

    Page<AgreementRequest> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<AgreementRequest> findByStatus(AgreementStatus status);

    Page<AgreementRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<AgreementRequest> findByStatusOrderByCreatedAtDesc(AgreementStatus status, Pageable pageable);

    Page<AgreementRequest> findByAgreementTypeOrderByCreatedAtDesc(com.safar.listing.entity.enums.AgreementType type, Pageable pageable);

    Page<AgreementRequest> findByStatusAndAgreementTypeOrderByCreatedAtDesc(AgreementStatus status, com.safar.listing.entity.enums.AgreementType type, Pageable pageable);
}
