package com.loanflow.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ledger_entries",
        indexes = {
                @Index(name = "idx_le_payment_id", columnList = "payment_id"),
                @Index(name = "idx_le_loan_id",    columnList = "loan_application_id"),
                @Index(name = "idx_le_entry_type", columnList = "entry_type"),
                @Index(name = "idx_le_created_at", columnList = "created_at")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LedgerEntry {

    @Id
    private String id;

    @Column(name = "payment_id", nullable = false)
    private String paymentId;

    @Column(name = "loan_application_id", nullable = false)
    private String loanApplicationId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false)
    private EntryType entryType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LedgerAccount account;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "running_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal runningBalance;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "correlation_id")
    private String correlationId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
    }

    public enum EntryType { DEBIT, CREDIT }

    public enum LedgerAccount {
        BORROWER_ACCOUNT,
        PRINCIPAL_RECEIVED,
        INTEREST_RECEIVED,
        PENALTY_RECEIVED,
        LOAN_DISBURSED
    }
}