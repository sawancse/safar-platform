package com.safar.listing.repository;

import com.safar.listing.entity.ChannelSyncLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ChannelSyncLogRepository extends JpaRepository<ChannelSyncLog, UUID> {

    Page<ChannelSyncLog> findByChannelManagerPropertyIdOrderBySyncedAtDesc(UUID propertyId, Pageable pageable);
}
