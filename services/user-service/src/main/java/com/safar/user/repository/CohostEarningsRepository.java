package com.safar.user.repository;

import com.safar.user.entity.CohostEarnings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CohostEarningsRepository extends JpaRepository<CohostEarnings, UUID> {

    List<CohostEarnings> findByAgreementIdIn(List<UUID> agreementIds);
}
