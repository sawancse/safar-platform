package com.safar.notification.repository;

import com.safar.notification.entity.GuestMilestone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GuestMilestoneRepository extends JpaRepository<GuestMilestone, UUID> {
    List<GuestMilestone> findByGuestIdOrderByAchievedAtDesc(UUID guestId);
    boolean existsByGuestIdAndMilestoneTypeAndMilestoneValue(UUID guestId, String type, int value);
    List<GuestMilestone> findByNotifiedFalse();
}
