package com.safar.user.entity;

import com.safar.user.entity.enums.PostCategory;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "nomad_posts", schema = "users")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class NomadPost {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID authorId;

    private String city;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PostCategory category;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(nullable = false)
    @Builder.Default
    private String tags = "";

    @Column(nullable = false)
    @Builder.Default
    private Integer upvotes = 0;

    @Column(nullable = false)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(nullable = false)
    @Builder.Default
    private Boolean pinned = false;

    @CreationTimestamp
    private OffsetDateTime createdAt;
}
