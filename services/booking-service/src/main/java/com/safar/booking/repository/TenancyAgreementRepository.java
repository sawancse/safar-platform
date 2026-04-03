package com.safar.booking.repository;

import com.safar.booking.entity.TenancyAgreement;
import com.safar.booking.entity.enums.AgreementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenancyAgreementRepository extends JpaRepository<TenancyAgreement, UUID> {

    Optional<TenancyAgreement> findByTenancyId(UUID tenancyId);

    Optional<TenancyAgreement> findByAgreementNumber(String agreementNumber);

    List<TenancyAgreement> findByStatus(AgreementStatus status);
}
