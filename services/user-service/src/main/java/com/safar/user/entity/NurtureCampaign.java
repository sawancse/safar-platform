package com.safar.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "nurture_campaigns", schema = "users")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class NurtureCampaign {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false) private String name;
    @Column(nullable = false) private String campaignType;
    private String targetSegment;
    private String subjectTemplate;
    private String emailTemplate;
    @Builder.Default private Integer delayHours = 0;
    @Builder.Default private Boolean active = true;
    @Builder.Default private Integer sentCount = 0;
    @Builder.Default private Integer openCount = 0;
    @Builder.Default private Integer clickCount = 0;
    @Builder.Default private Integer conversionCount = 0;
    @CreationTimestamp private OffsetDateTime createdAt;
}
