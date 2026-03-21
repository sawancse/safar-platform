package com.safar.notification.controller;

import com.safar.notification.dto.NotificationResponse;
import com.safar.notification.dto.UnreadCountResponse;
import com.safar.notification.service.InAppNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final InAppNotificationService inAppNotificationService;

    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> getNotifications(
            @RequestHeader("X-User-Id") UUID userId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(inAppNotificationService.getNotifications(userId, pageable));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(inAppNotificationService.getUnreadCount(userId));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId) {
        inAppNotificationService.markAsRead(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(
            @RequestHeader("X-User-Id") UUID userId) {
        inAppNotificationService.markAllAsRead(userId);
        return ResponseEntity.noContent().build();
    }
}
