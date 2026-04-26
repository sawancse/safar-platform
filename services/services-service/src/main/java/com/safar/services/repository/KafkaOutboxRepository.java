package com.safar.services.repository;

import com.safar.services.entity.KafkaOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface KafkaOutboxRepository extends JpaRepository<KafkaOutbox, UUID> {
    List<KafkaOutbox> findByStatusAndRetryCountLessThanOrderByCreatedAtAsc(String status, int maxRetries);
    long countByStatus(String status);
}
