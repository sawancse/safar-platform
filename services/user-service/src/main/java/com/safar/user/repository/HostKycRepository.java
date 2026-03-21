package com.safar.user.repository;

import com.safar.user.entity.HostKyc;
import com.safar.user.entity.enums.KycStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HostKycRepository extends JpaRepository<HostKyc, UUID> {
    Optional<HostKyc> findByUserId(UUID userId);
    List<HostKyc> findByStatus(KycStatus status);
    List<HostKyc> findByStatusNot(KycStatus status);
    List<HostKyc> findByStatusAndUserId(KycStatus status, UUID userId);
    Optional<HostKyc> findByAadhaarNumber(String aadhaarNumber);
    Optional<HostKyc> findByPanNumber(String panNumber);
    Optional<HostKyc> findByBankAccountNumber(String bankAccountNumber);
}
