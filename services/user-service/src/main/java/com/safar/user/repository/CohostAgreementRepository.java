package com.safar.user.repository;

import com.safar.user.entity.CohostAgreement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CohostAgreementRepository extends JpaRepository<CohostAgreement, UUID> {

    List<CohostAgreement> findByHostIdOrCohostId(UUID hostId, UUID cohostId);
}
