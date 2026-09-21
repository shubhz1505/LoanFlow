package com.loanflow.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments",
        indexes = {
                @Index(name = "idx_pay_loan_id", columnList = "loan_application_id"),
                @Index(name = "idx_pay_user_id", columnList = "user_id"),
                @Index(name = "idx_pay_status",  columnList = "status"),
                @Index(name = "idx_pay_txn_ref", columnList = "transaction_reference")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Payment {

    @Id
    private String id;

    @Column(name = "loan_application_id", nullable = false)
    private String loanApplicationId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "principal_component", nullable = false, precision = 15, scale = 2)
    private BigDecimal principalComponent;

    @Column(name = "interest_component", nullable = false, precision = 15, scale = 2)
    private BigDecimal interestComponent;

    @Column(name = "penalty_component", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal penaltyComponent = BigDecimal.ZERO;

    @Column(name = "outstanding_balance_after", nullable = false, precision = 15, scale = 2)
    private BigDecimal outstandingBalanceAfter;

    @Column(name = "emi_number")
    private Integer emiNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode", nullable = false)
    private PaymentMode paymentMode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.SUCCESS;

    @Column(name = "transaction_reference", unique = true)
    private String transactionReference;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "correlation_id")
    private String correlationId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = "PAY-" + UUID.randomUUID()
                    .toString().substring(0, 8).toUpperCase();
        }
        if (this.status == null) this.status = PaymentStatus.SUCCESS;
        if (this.penaltyComponent == null)
            this.penaltyComponent = BigDecimal.ZERO;
    }
}