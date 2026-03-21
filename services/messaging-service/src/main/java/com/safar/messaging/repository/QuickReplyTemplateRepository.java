package com.safar.messaging.repository;

import com.safar.messaging.entity.QuickReplyTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuickReplyTemplateRepository extends JpaRepository<QuickReplyTemplate, UUID> {

    List<QuickReplyTemplate> findByUserIdOrderBySortOrder(UUID userId);
}
