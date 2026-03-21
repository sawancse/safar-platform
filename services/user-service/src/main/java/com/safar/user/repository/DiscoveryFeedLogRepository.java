package com.safar.user.repository;

import com.safar.user.entity.DiscoveryFeedLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DiscoveryFeedLogRepository extends JpaRepository<DiscoveryFeedLog, UUID> {
}
