package com.safar.chef.service;

import com.safar.chef.entity.KafkaOutbox;
import com.safar.chef.repository.KafkaOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResilientKafkaService {
    private final KafkaTemplate<String, String> kafka;
    private final KafkaOutboxRepository outboxRepo;

    public void send(String topic, String key, String payload) {
        try {
            kafka.send(topic, key, payload).get(3, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Kafka send failed for topic={}, key={}. Persisting to outbox.", topic, key);
            outboxRepo.save(KafkaOutbox.builder().topic(topic).eventKey(key).payload(payload).build());
        }
    }

    public void send(String topic, String payload) { send(topic, null, payload); }

    @Scheduled(fixedRate = 30000)
    @Transactional
    public void retryPendingOutbox() {
        List<KafkaOutbox> pending = outboxRepo.findByStatusAndRetryCountLessThanOrderByCreatedAtAsc("PENDING", 10);
        if (pending.isEmpty()) return;
        int sent = 0, failed = 0;
        for (KafkaOutbox entry : pending) {
            try {
                if (entry.getEventKey() != null) kafka.send(entry.getTopic(), entry.getEventKey(), entry.getPayload()).get(3, java.util.concurrent.TimeUnit.SECONDS);
                else kafka.send(entry.getTopic(), entry.getPayload()).get(3, java.util.concurrent.TimeUnit.SECONDS);
                entry.setStatus("SENT"); entry.setSentAt(OffsetDateTime.now()); outboxRepo.save(entry); sent++;
            } catch (Exception e) {
                entry.setRetryCount(entry.getRetryCount() + 1); entry.setErrorMessage(e.getMessage());
                if (entry.getRetryCount() >= entry.getMaxRetries()) { entry.setStatus("FAILED"); }
                outboxRepo.save(entry); failed++;
            }
        }
        log.info("Outbox retry: {} sent, {} failed", sent, failed);
    }
}
