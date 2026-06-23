package com.safar.insurance.repository;

import com.safar.insurance.entity.InsurancePolicy;
import com.safar.insurance.entity.enums.PolicyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InsurancePolicyRepository extends JpaRepository<InsurancePolicy, UUID> {

    Page<InsurancePolicy> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<InsurancePolicy> findByStatusAndTripEndDate(PolicyStatus status, LocalDate tripEndDate);

    Optional<InsurancePolicy> findByPolicyRef(String policyRef);

    Optional<InsurancePolicy> findByProviderAndExternalPolicyId(String provider, String externalPolicyId);

    Optional<InsurancePolicy> findFirstByBookingId(UUID bookingId);

    long countByStatus(PolicyStatus status);
}
