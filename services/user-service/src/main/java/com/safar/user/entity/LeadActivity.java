package com.safar.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "lead_activities", schema = "users")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class LeadActivity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private UUID leadId;
    private String email;
    @Column(nullable = false) private String activityType;
    @Column(columnDefinition = "TEXT") private String metadata;
    @Builder.Default private Integer scoreDelta = 0;
    @CreationTimestamp private OffsetDateTime createdAt;
}
