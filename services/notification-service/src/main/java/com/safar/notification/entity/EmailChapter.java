package com.safar.notification.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "email_chapters", schema = "notifications")
public class EmailChapter {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID bookingId;

    @Column(nullable = false)
    private UUID guestId;

    @Column(nullable = false)
    private UUID hostId;

    @Column(nullable = false)
    private UUID listingId;

    @Column(nullable = false)
    private Integer chapterNumber;

    @Column(nullable = false, length = 100)
    private String chapterName;

    @Column(nullable = false)
    private OffsetDateTime sentAt = OffsetDateTime.now();

    @Column(nullable = false)
    private String emailTo;

    @Column(nullable = false, length = 20)
    private String tone = "FORMAL";

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getBookingId() { return bookingId; }
    public void setBookingId(UUID bookingId) { this.bookingId = bookingId; }
    public UUID getGuestId() { return guestId; }
    public void setGuestId(UUID guestId) { this.guestId = guestId; }
    public UUID getHostId() { return hostId; }
    public void setHostId(UUID hostId) { this.hostId = hostId; }
    public UUID getListingId() { return listingId; }
    public void setListingId(UUID listingId) { this.listingId = listingId; }
    public Integer getChapterNumber() { return chapterNumber; }
    public void setChapterNumber(Integer chapterNumber) { this.chapterNumber = chapterNumber; }
    public String getChapterName() { return chapterName; }
    public void setChapterName(String chapterName) { this.chapterName = chapterName; }
    public OffsetDateTime getSentAt() { return sentAt; }
    public void setSentAt(OffsetDateTime sentAt) { this.sentAt = sentAt; }
    public String getEmailTo() { return emailTo; }
    public void setEmailTo(String emailTo) { this.emailTo = emailTo; }
    public String getTone() { return tone; }
    public void setTone(String tone) { this.tone = tone; }
}
