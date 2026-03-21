package com.safar.notification.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "scheduled_emails", schema = "notifications")
public class ScheduledEmail {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID bookingId;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 50)
    private String emailType;

    @Column(nullable = false)
    private OffsetDateTime scheduledFor;

    @Column(nullable = false)
    private boolean sent = false;

    private OffsetDateTime sentAt;

    @Column(nullable = false)
    private boolean cancelled = false;

    @Column(columnDefinition = "TEXT")
    private String contextJson;

    @Column(nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getBookingId() { return bookingId; }
    public void setBookingId(UUID bookingId) { this.bookingId = bookingId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getEmailType() { return emailType; }
    public void setEmailType(String emailType) { this.emailType = emailType; }
    public OffsetDateTime getScheduledFor() { return scheduledFor; }
    public void setScheduledFor(OffsetDateTime scheduledFor) { this.scheduledFor = scheduledFor; }
    public boolean isSent() { return sent; }
    public void setSent(boolean sent) { this.sent = sent; }
    public OffsetDateTime getSentAt() { return sentAt; }
    public void setSentAt(OffsetDateTime sentAt) { this.sentAt = sentAt; }
    public boolean isCancelled() { return cancelled; }
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
    public String getContextJson() { return contextJson; }
    public void setContextJson(String contextJson) { this.contextJson = contextJson; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
