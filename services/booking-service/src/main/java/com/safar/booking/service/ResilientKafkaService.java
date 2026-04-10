package com.safar.booking.service;

import com.safar.booking.entity.KafkaOutbox;
import com.safar.booking.repository.KafkaOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Resilient Kafka producer with outbox pattern.
 * - Tries to send via Kafka immediately
 * - On failure, persists to outbox table
 * - Scheduler retries pending outbox entries every 30 seconds
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResilientKafkaService {

    private final KafkaTemplate<String, String> kafka;
    private final KafkaOutboxRepository outboxRepo;

    /**
     * Send Kafka event with outbox fallback.
     * If Kafka is down, the event is persisted and retried later.
     */
    public void send(String topic, String key, String payload) {
        try {
            kafka.send(topic, key, payload).get(3, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Kafka send failed for topic={}, key={}. Persisting to outbox. Error: {}", topic, key, e.getMessage());
            outboxRepo.save(KafkaOutbox.builder()
                    .topic(topic)
                    .eventKey(key)
                    .payload(payload)
                    .build());
        }
    }

    /**
     * Send without key.
     */
    public void send(String topic, String payload) {
        send(topic, null, payload);
    }

    /**
     * Retry pending outbox entries every 30 seconds.
     */
    @Scheduled(fixedRate = 30000)
    @Transactional
    public void retryPendingOutbox() {
        List<KafkaOutbox> pending = outboxRepo.findByStatusAndRetryCountLessThanOrderByCreatedAtAsc("PENDING", 10);
        if (pending.isEmpty()) return;

        int sent = 0, failed = 0;
        for (KafkaOutbox entry : pending) {
            try {
                if (entry.getEventKey() != null) {
                    kafka.send(entry.getTopic(), entry.getEventKey(), entry.getPayload())
                            .get(3, java.util.concurrent.TimeUnit.SECONDS);
                } else {
                    kafka.send(entry.getTopic(), entry.getPayload())
                            .get(3, java.util.concurrent.TimeUnit.SECONDS);
                }
                entry.setStatus("SENT");
                entry.setSentAt(OffsetDateTime.now());
                outboxRepo.save(entry);
                sent++;
            } catch (Exception e) {
                entry.setRetryCount(entry.getRetryCount() + 1);
                entry.setErrorMessage(e.getMessage());
                if (entry.getRetryCount() >= entry.getMaxRetries()) {
                    entry.setStatus("FAILED");
                    log.error("Outbox entry permanently failed after {} retries: topic={}, key={}",
                            entry.getRetryCount(), entry.getTopic(), entry.getEventKey());
                }
                outboxRepo.save(entry);
                failed++;
            }
        }
        log.info("Outbox retry: {} sent, {} failed, {} remaining",
                sent, failed, pending.size() - sent - failed);
    }
}
