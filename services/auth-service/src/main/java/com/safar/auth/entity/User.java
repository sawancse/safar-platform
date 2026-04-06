package com.safar.auth.entity;

import com.safar.auth.entity.enums.KycStatus;
import com.safar.auth.entity.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "users", schema = "auth")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, length = 13)
    private String phone;

    @Column(unique = true)
    private String email;

    @Column(unique = true)
    private String googleId;

    @Column(unique = true)
    private String appleId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserRole role = UserRole.GUEST;

    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private KycStatus kycStatus = KycStatus.PENDING;

    private String aadhaarRef;
    private String panRef;

    private String passwordHash;

    private OffsetDateTime passwordSetAt;

    @Builder.Default
    private int failedLoginAttempts = 0;

    private OffsetDateTime lockedUntil;

    // PIN-based quick login (HDFC-style)
    private String pinHash;

    private OffsetDateTime pinSetAt;

    @Builder.Default
    private int pinFailedAttempts = 0;

    private OffsetDateTime pinLockedUntil;

    public boolean hasPin() {
        return pinHash != null && !pinHash.isBlank();
    }

    public boolean isPinLocked() {
        return pinLockedUntil != null && pinLockedUntil.isAfter(OffsetDateTime.now());
    }

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String language = "en";

    @Column(nullable = false)
    @Builder.Default
    private boolean isActive = true;

    public boolean hasPassword() {
        return passwordHash != null && !passwordHash.isBlank();
    }

    public boolean isLocked() {
        return lockedUntil != null && lockedUntil.isAfter(OffsetDateTime.now());
    }

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private OffsetDateTime updatedAt;
}
