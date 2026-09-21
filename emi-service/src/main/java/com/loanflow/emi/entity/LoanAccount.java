package com.loanflow.emi.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "loan_accounts",
        indexes = {
                @Index(name = "idx_la_user_id", columnList = "user_id"),
                @Index(name = "idx_la_status",  columnList = "account_status")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LoanAccount {

    @Id
    @Column(name = "loan_application_id")
    private String loanApplicationId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "principal_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal principalAmount;

    @Column(name = "outstanding_principal", nullable = false, precision = 15, scale = 2)
    private BigDecimal outstandingPrincipal;

    @Column(name = "total_interest_payable", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalInterestPayable;

    @Column(name = "total_interest_paid", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalInterestPaid = BigDecimal.ZERO;

    @Column(name = "total_principal_paid", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalPrincipalPaid = BigDecimal.ZERO;

    @Column(name = "total_penalty_charged", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalPenaltyCharged = BigDecimal.ZERO;

    @Column(name = "annual_interest_rate", nullable = false, precision = 6, scale = 3)
    private BigDecimal annualInterestRate;

    @Column(name = "tenure_months", nullable = false)
    private Integer tenureMonths;

    @Column(name = "emis_paid", nullable = false)
    @Builder.Default
    private Integer emisPaid = 0;

    @Column(name = "emis_remaining", nullable = false)
    private Integer emisRemaining;

    @Column(name = "emi_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal emiAmount;

    @Column(name = "first_emi_date")
    private LocalDate firstEmiDate;

    @Column(name = "last_emi_date")
    private LocalDate lastEmiDate;

    @Column(name = "next_emi_due_date")
    private LocalDate nextEmiDueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false)
    @Builder.Default
    private AccountStatus accountStatus = AccountStatus.ACTIVE;

    @UpdateTimestamp
    @Column(name = "last_updated_at")
    private LocalDateTime lastUpdatedAt;
}