package com.safar.notification.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "festival_calendar", schema = "notifications")
public class FestivalCampaign {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String festivalName;

    @Column(nullable = false)
    private LocalDate festivalDate;

    @Column(length = 50)
    private String region;

    @Column(length = 10)
    private String languageCode;

    @Column(nullable = false)
    private String campaignSubject;

    @Column(nullable = false)
    private String campaignHeadline;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String campaignBody;

    @Column(length = 500)
    private String discoveryCategories;

    @Column(length = 500)
    private String targetCities;

    @Column(nullable = false)
    private boolean isActive = true;

    @Column(nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getFestivalName() { return festivalName; }
    public void setFestivalName(String festivalName) { this.festivalName = festivalName; }
    public LocalDate getFestivalDate() { return festivalDate; }
    public void setFestivalDate(LocalDate festivalDate) { this.festivalDate = festivalDate; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getLanguageCode() { return languageCode; }
    public void setLanguageCode(String languageCode) { this.languageCode = languageCode; }
    public String getCampaignSubject() { return campaignSubject; }
    public void setCampaignSubject(String campaignSubject) { this.campaignSubject = campaignSubject; }
    public String getCampaignHeadline() { return campaignHeadline; }
    public void setCampaignHeadline(String campaignHeadline) { this.campaignHeadline = campaignHeadline; }
    public String getCampaignBody() { return campaignBody; }
    public void setCampaignBody(String campaignBody) { this.campaignBody = campaignBody; }
    public String getDiscoveryCategories() { return discoveryCategories; }
    public void setDiscoveryCategories(String discoveryCategories) { this.discoveryCategories = discoveryCategories; }
    public String getTargetCities() { return targetCities; }
    public void setTargetCities(String targetCities) { this.targetCities = targetCities; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
