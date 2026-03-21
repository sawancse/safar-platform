package com.safar.notification.service;

import com.safar.notification.dto.NotificationResponse;
import com.safar.notification.dto.UnreadCountResponse;
import com.safar.notification.entity.Notification;
import com.safar.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InAppNotificationService {

    private final NotificationRepository notificationRepository;

    public void create(UUID userId, String title, String body, String type,
                       String referenceId, String referenceType) {
        try {
            Notification notification = Notification.builder()
                    .userId(userId)
                    .title(title)
                    .body(body)
                    .type(type)
                    .referenceId(referenceId)
                    .referenceType(referenceType)
                    .build();
            notificationRepository.save(notification);
            log.debug("In-app notification created for user {} — type={}", userId, type);
        } catch (Exception e) {
            log.error("Failed to create in-app notification for user {}: {}", userId, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(UUID userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(UUID userId) {
        long count = notificationRepository.countByUserIdAndReadFalse(userId);
        return new UnreadCountResponse(count);
    }

    @Transactional
    public void markAsRead(UUID notificationId, UUID userId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            if (n.getUserId().equals(userId)) {
                n.setRead(true);
                notificationRepository.save(n);
            }
        });
    }

    @Transactional
    public int markAllAsRead(UUID userId) {
        return notificationRepository.markAllAsRead(userId);
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getTitle(),
                n.getBody(),
                n.getType(),
                n.getReferenceId(),
                n.getReferenceType(),
                n.isRead(),
                n.getCreatedAt()
        );
    }
}
