package com.safar.notification.repository;

import com.safar.notification.entity.EmailPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EmailPreferenceRepository extends JpaRepository<EmailPreference, UUID> {
    Optional<EmailPreference> findByUserId(UUID userId);
}
