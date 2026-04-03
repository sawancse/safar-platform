package com.safar.user.entity;

import com.safar.user.entity.enums.ReferralStatus;
import com.safar.user.entity.enums.ReferralType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "referrals", schema = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Referral {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "referrer_id", nullable = false)
    private UUID referrerId;

    @Column(name = "referred_id")
    private UUID referredId;

    @Column(name = "referral_code", nullable = false, unique = true, length = 12)
    private String referralCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ReferralType type = ReferralType.GUEST;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ReferralStatus status = ReferralStatus.PENDING;

    @Column(name = "referrer_reward_paise")
    @Builder.Default
    private Long referrerRewardPaise = 50000L; // ₹500 default

    @Column(name = "referred_reward_paise")
    @Builder.Default
    private Long referredRewardPaise = 25000L; // ₹250 default

    @Column(name = "referrer_credited")
    @Builder.Default
    private Boolean referrerCredited = false;

    @Column(name = "referred_credited")
    @Builder.Default
    private Boolean referredCredited = false;

    @Column(name = "qualifying_booking_id")
    private UUID qualifyingBookingId;

    @CreationTimestamp
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;
}
