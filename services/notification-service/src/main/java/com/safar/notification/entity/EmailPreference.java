package com.safar.notification.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "email_preferences", schema = "notifications")
public class EmailPreference {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID userId;

    @Column(length = 20)
    private String tonePreference = "AUTO";

    @Column(nullable = false)
    private boolean marketingEmails = true;

    @Column(nullable = false)
    private boolean milestoneEmails = true;

    @Column(nullable = false)
    private boolean midStayChecks = true;

    @Column(nullable = false)
    private boolean reEngagementEmails = true;

    @Column(nullable = false)
    private boolean festivalEmails = true;

    @Column(nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getTonePreference() { return tonePreference; }
    public void setTonePreference(String tonePreference) { this.tonePreference = tonePreference; }
    public boolean isMarketingEmails() { return marketingEmails; }
    public void setMarketingEmails(boolean marketingEmails) { this.marketingEmails = marketingEmails; }
    public boolean isMilestoneEmails() { return milestoneEmails; }
    public void setMilestoneEmails(boolean milestoneEmails) { this.milestoneEmails = milestoneEmails; }
    public boolean isMidStayChecks() { return midStayChecks; }
    public void setMidStayChecks(boolean midStayChecks) { this.midStayChecks = midStayChecks; }
    public boolean isReEngagementEmails() { return reEngagementEmails; }
    public void setReEngagementEmails(boolean reEngagementEmails) { this.reEngagementEmails = reEngagementEmails; }
    public boolean isFestivalEmails() { return festivalEmails; }
    public void setFestivalEmails(boolean festivalEmails) { this.festivalEmails = festivalEmails; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
