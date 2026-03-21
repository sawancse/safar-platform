package com.safar.notification.repository;

import com.safar.notification.entity.HostMilestone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface HostMilestoneRepository extends JpaRepository<HostMilestone, UUID> {
    List<HostMilestone> findByHostIdOrderByAchievedAtDesc(UUID hostId);
    boolean existsByHostIdAndMilestoneTypeAndMilestoneValue(UUID hostId, String type, int value);
    List<HostMilestone> findByNotifiedFalse();
}
