package com.safar.notification.kafka;

import com.safar.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReviewEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = "review.created", groupId = "notification-review-group")
    public void onReviewCreated(String message) {
        try {
            notificationService.notifyReviewCreated(message);
        } catch (Exception e) {
            log.error("Error handling review.created event: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "review.replied", groupId = "notification-review-group")
    public void onReviewReplied(String message) {
        try {
            notificationService.notifyReviewReplied(message);
        } catch (Exception e) {
            log.error("Error handling review.replied event: {}", e.getMessage());
        }
    }
}
