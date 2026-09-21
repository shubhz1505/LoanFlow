package com.loanflow.document.entity;

import com.loanflow.commons.enums.DocumentType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "loan_documents",
        indexes = {
                @Index(name = "idx_doc_loan_id",  columnList = "loan_application_id"),
                @Index(name = "idx_doc_user_id",  columnList = "user_id"),
                @Index(name = "idx_doc_type",     columnList = "document_type"),
                @Index(name = "idx_doc_verified", columnList = "verified")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LoanDocument {

    @Id
    private String id;

    @Column(name = "loan_application_id", nullable = false)
    private String loanApplicationId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false)
    private DocumentType documentType;

    @Column(name = "bucket_name")
    private String bucketName;

    @Column(name = "object_key", length = 500)
    private String objectKey;

    @Column(name = "original_file_name")
    private String originalFileName;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "extracted_text", columnDefinition = "TEXT")
    private String extractedText;

    @Column(name = "extracted_income")
    private String extractedIncome;

    @Column(name = "income_verified")
    private Boolean incomeVerified;

    @Column(name = "verified", nullable = false)
    @Builder.Default
    private Boolean verified = false;

    @Column(name = "verification_note", columnDefinition = "TEXT")
    private String verificationNote;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "verified_by")
    private String verifiedBy;

    @Column(name = "rejected", nullable = false)
    @Builder.Default
    private Boolean rejected = false;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "correlation_id")
    private String correlationId;

    @CreationTimestamp
    @Column(name = "uploaded_at", updatable = false)
    private LocalDateTime uploadedAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null)       this.id       = UUID.randomUUID().toString();
        if (this.verified == null) this.verified  = false;
        if (this.rejected == null) this.rejected  = false;
    }
}