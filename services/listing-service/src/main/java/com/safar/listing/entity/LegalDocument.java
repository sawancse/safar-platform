package com.safar.listing.entity;

import com.safar.listing.entity.enums.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "legal_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LegalDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "case_id", nullable = false)
    private UUID legalCaseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false)
    private LegalDocType docType;

    @Column(nullable = false, length = 200)
    private String fileName;

    @Column(nullable = false)
    private String fileUrl;

    private Long fileSizeBytes;

    @Column(length = 50)
    private String mimeType;

    @Builder.Default
    private Boolean uploadedByUser = true;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @CreationTimestamp
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
