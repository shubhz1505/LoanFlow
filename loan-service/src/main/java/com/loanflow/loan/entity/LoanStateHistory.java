package com.loanflow.loan.entity;

import com.loanflow.commons.enums.LoanStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "loan_state_history",
        indexes = {
                @Index(name = "idx_lsh_loan_id", columnList = "loan_application_id")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LoanStateHistory {

    @Id
    private String id;

    @Column(name = "loan_application_id", nullable = false)
    private String loanApplicationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", nullable = false)
    private LoanStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false)
    private LoanStatus toStatus;

    @Column(name = "performed_by")
    private String performedBy;

    @Column(name = "performed_by_role")
    private String performedByRole;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "correlation_id")
    private String correlationId;

    @CreationTimestamp
    @Column(name = "transitioned_at", updatable = false, nullable = false)
    private LocalDateTime transitionedAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
    }
}