package com.safar.services.repository;

import com.safar.services.entity.CommissionRateConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommissionRateConfigRepository extends JpaRepository<CommissionRateConfigEntity, UUID> {

    Optional<CommissionRateConfigEntity> findByServiceTypeAndTier(String serviceType, String tier);

    List<CommissionRateConfigEntity> findByServiceType(String serviceType);
}
