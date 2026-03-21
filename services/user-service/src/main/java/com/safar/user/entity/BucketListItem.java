package com.safar.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "guest_bucket_list", schema = "users")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BucketListItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID guestId;

    @Column(nullable = false)
    private UUID listingId;

    @CreationTimestamp
    private OffsetDateTime addedAt;

    private String notes;
}
