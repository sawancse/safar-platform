package com.safar.payment.service;

import com.safar.payment.entity.ProcessedWebhook;
import com.safar.payment.repository.ProcessedWebhookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {

    private final ProcessedWebhookRepository processedWebhookRepo;

    /**
     * Checks if a webhook event has already been processed.
     */
    public boolean isProcessed(String gatewayEventId) {
        return processedWebhookRepo.findByGatewayEventId(gatewayEventId)
                .map(w -> "COMPLETED".equals(w.getStatus()))
                .orElse(false);
    }

    /**
     * Marks a webhook event as processing. Uses unique constraint for deduplication.
     *
     * @return true if this is a new event (inserted successfully), false if already exists
     */
    @Transactional
    public boolean markProcessing(String gatewayEventId, String gateway, String eventType) {
        if (processedWebhookRepo.findByGatewayEventId(gatewayEventId).isPresent()) {
            log.info("Webhook event already exists: {}", gatewayEventId);
            return false;
        }

        try {
            ProcessedWebhook webhook = ProcessedWebhook.builder()
                    .gatewayEventId(gatewayEventId)
                    .gateway(gateway)
                    .eventType(eventType)
                    .status("PROCESSING")
                    .build();
            processedWebhookRepo.save(webhook);
            log.info("Webhook event marked as processing: {}", gatewayEventId);
            return true;
        } catch (DataIntegrityViolationException e) {
            // Concurrent insert — another thread already processed this event
            log.info("Webhook event already being processed (concurrent): {}", gatewayEventId);
            return false;
        }
    }

    /**
     * Marks a webhook event as completed.
     */
    @Transactional
    public void markCompleted(String gatewayEventId) {
        processedWebhookRepo.findByGatewayEventId(gatewayEventId).ifPresent(webhook -> {
            webhook.setStatus("COMPLETED");
            processedWebhookRepo.save(webhook);
            log.info("Webhook event marked as completed: {}", gatewayEventId);
        });
    }
}
