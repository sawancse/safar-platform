package com.safar.booking.repository;

import com.safar.booking.entity.MaintenanceRequest;
import com.safar.booking.entity.enums.MaintenanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MaintenanceRequestRepository extends JpaRepository<MaintenanceRequest, UUID> {

    Page<MaintenanceRequest> findByTenancyIdOrderByCreatedAtDesc(UUID tenancyId, Pageable pageable);

    Page<MaintenanceRequest> findByTenancyIdAndStatusOrderByCreatedAtDesc(
            UUID tenancyId, MaintenanceStatus status, Pageable pageable);

    long countByTenancyIdAndStatus(UUID tenancyId, MaintenanceStatus status);
}
