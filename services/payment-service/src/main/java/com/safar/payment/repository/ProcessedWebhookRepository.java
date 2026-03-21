package com.safar.payment.repository;

import com.safar.payment.entity.ProcessedWebhook;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProcessedWebhookRepository extends JpaRepository<ProcessedWebhook, UUID> {
    Optional<ProcessedWebhook> findByGatewayEventId(String gatewayEventId);
}
