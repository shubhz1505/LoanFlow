package com.loanflow.emi.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "emi_schedule",
        indexes = {
                @Index(name = "idx_emi_loan_id",  columnList = "loan_application_id"),
                @Index(name = "idx_emi_due_date", columnList = "due_date"),
                @Index(name = "idx_emi_status",   columnList = "status")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmiSchedule {

    @Id
    private String id;

    @Column(name = "loan_application_id", nullable = false)
    private String loanApplicationId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "emi_number", nullable = false)
    private Integer emiNumber;

    @Column(name = "total_emis", nullable = false)
    private Integer totalEmis;

    @Column(name = "emi_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal emiAmount;

    @Column(name = "principal_component", nullable = false, precision = 15, scale = 2)
    private BigDecimal principalComponent;

    @Column(name = "interest_component", nullable = false, precision = 15, scale = 2)
    private BigDecimal interestComponent;

    @Column(name = "outstanding_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal outstandingBalance;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private EmiStatus status = EmiStatus.PENDING;

    @Column(name = "paid_date")
    private LocalDate paidDate;

    @Column(name = "amount_paid", precision = 15, scale = 2)
    private BigDecimal amountPaid;

    @Column(name = "penalty_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal penaltyAmount = BigDecimal.ZERO;

    @Column(name = "payment_id")
    private String paymentId;

    @Column(name = "overdue_days")
    @Builder.Default
    private Integer overdueDays = 0;

    @Column(name = "reminder_sent")
    @Builder.Default
    private Boolean reminderSent = false;

    @Column(name = "reminder_sent_at")
    private LocalDateTime reminderSentAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
        if (this.status == null) this.status = EmiStatus.PENDING;
        if (this.penaltyAmount == null) this.penaltyAmount = BigDecimal.ZERO;
        if (this.overdueDays == null) this.overdueDays = 0;
        if (this.reminderSent == null) this.reminderSent = false;
    }
}