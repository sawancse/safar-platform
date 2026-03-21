package com.safar.notification.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "guest_milestones", schema = "notifications")
public class GuestMilestone {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID guestId;

    @Column(nullable = false, length = 50)
    private String milestoneType;

    @Column(nullable = false)
    private Integer milestoneValue;

    @Column(nullable = false)
    private OffsetDateTime achievedAt = OffsetDateTime.now();

    @Column(nullable = false)
    private boolean notified = false;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getGuestId() { return guestId; }
    public void setGuestId(UUID guestId) { this.guestId = guestId; }
    public String getMilestoneType() { return milestoneType; }
    public void setMilestoneType(String milestoneType) { this.milestoneType = milestoneType; }
    public Integer getMilestoneValue() { return milestoneValue; }
    public void setMilestoneValue(Integer milestoneValue) { this.milestoneValue = milestoneValue; }
    public OffsetDateTime getAchievedAt() { return achievedAt; }
    public void setAchievedAt(OffsetDateTime achievedAt) { this.achievedAt = achievedAt; }
    public boolean isNotified() { return notified; }
    public void setNotified(boolean notified) { this.notified = notified; }
}
