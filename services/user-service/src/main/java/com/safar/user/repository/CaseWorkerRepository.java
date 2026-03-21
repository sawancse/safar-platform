package com.safar.user.repository;

import com.safar.user.entity.CaseWorker;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CaseWorkerRepository extends JpaRepository<CaseWorker, UUID> {
    List<CaseWorker> findByOrganizationId(UUID organizationId);
    List<CaseWorker> findByUserId(UUID userId);
}
