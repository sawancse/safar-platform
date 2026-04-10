package com.safar.chef.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "kafka_outbox", schema = "chefs")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class KafkaOutbox {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false, length = 200) private String topic;
    @Column(length = 200) private String eventKey;
    @Column(nullable = false, columnDefinition = "TEXT") private String payload;
    @Column(length = 20) @Builder.Default private String status = "PENDING";
    @Builder.Default private Integer retryCount = 0;
    @Builder.Default private Integer maxRetries = 10;
    @Column(columnDefinition = "TEXT") private String errorMessage;
    @CreationTimestamp private OffsetDateTime createdAt;
    private OffsetDateTime sentAt;
}
