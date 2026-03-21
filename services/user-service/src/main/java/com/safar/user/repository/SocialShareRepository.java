package com.safar.user.repository;

import com.safar.user.entity.SocialShare;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SocialShareRepository extends JpaRepository<SocialShare, UUID> {
    List<SocialShare> findByGuestId(UUID guestId);
}
