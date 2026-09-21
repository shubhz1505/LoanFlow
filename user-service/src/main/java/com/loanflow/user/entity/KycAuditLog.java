package com.loanflow.user.entity;

import com.loanflow.commons.enums.KycStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "kyc_audit_log",
        indexes = {
                @Index(name = "idx_kyc_log_user_id", columnList = "user_id")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class KycAuditLog {

    @Id
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", nullable = false)
    private KycStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false)
    private KycStatus toStatus;

    @Column(name = "performed_by")
    private String performedBy;

    @Column(name = "performed_by_role")
    private String performedByRole;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "ip_address")
    private String ipAddress;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
    }
}