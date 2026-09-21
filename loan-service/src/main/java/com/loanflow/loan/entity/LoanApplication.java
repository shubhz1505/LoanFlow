package com.loanflow.loan.entity;

import com.loanflow.commons.enums.EmploymentType;
import com.loanflow.commons.enums.LoanStatus;
import com.loanflow.commons.enums.LoanType;
import com.loanflow.commons.enums.RiskTier;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "loan_applications",
        indexes = {
                @Index(name = "idx_loan_user_id",   columnList = "user_id"),
                @Index(name = "idx_loan_status",     columnList = "status"),
                @Index(name = "idx_loan_type",       columnList = "loan_type"),
                @Index(name = "idx_loan_created_at", columnList = "created_at"),
                @Index(name = "idx_loan_maker",      columnList = "maker_officer_id")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LoanApplication {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Column(name = "user_full_name", nullable = false)
    private String userFullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "loan_type", nullable = false)
    private LoanType loanType;

    @Column(name = "requested_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal requestedAmount;

    @Column(name = "tenure_months", nullable = false)
    private Integer tenureMonths;

    @Column(name = "monthly_income", nullable = false, precision = 15, scale = 2)
    private BigDecimal monthlyIncome;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", nullable = false)
    private EmploymentType employmentType;

    @Column(name = "existing_emi_amount", precision = 15, scale = 2)
    private BigDecimal existingEmiAmount;

    @Column(name = "existing_loan_count")
    private Integer existingLoanCount;

    @Column(name = "loan_purpose", columnDefinition = "TEXT")
    private String loanPurpose;

    // Credit assessment
    @Column(name = "credit_score")
    private Integer creditScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_tier")
    private RiskTier riskTier;

    @Column(name = "max_eligible_amount", precision = 15, scale = 2)
    private BigDecimal maxEligibleAmount;

    @Column(name = "recommended_interest_rate", precision = 6, scale = 3)
    private BigDecimal recommendedInterestRate;

    @Column(name = "credit_manual_review")
    private Boolean creditManualReview;

    // Maker-Checker
    @Column(name = "maker_officer_id")
    private String makerOfficerId;

    @Column(name = "maker_decision")
    private String makerDecision;

    @Column(name = "maker_remarks", columnDefinition = "TEXT")
    private String makerRemarks;

    @Column(name = "maker_decision_at")
    private LocalDateTime makerDecisionAt;

    @Column(name = "checker_officer_id")
    private String checkerOfficerId;

    @Column(name = "checker_decision_at")
    private LocalDateTime checkerDecisionAt;

    @Column(name = "checker_remarks", columnDefinition = "TEXT")
    private String checkerRemarks;

    // Approved terms
    @Column(name = "approved_amount", precision = 15, scale = 2)
    private BigDecimal approvedAmount;

    @Column(name = "approved_interest_rate", precision = 6, scale = 3)
    private BigDecimal approvedInterestRate;

    @Column(name = "approved_tenure_months")
    private Integer approvedTenureMonths;

    @Column(name = "emi_amount", precision = 15, scale = 2)
    private BigDecimal emiAmount;

    @Column(name = "disbursement_date")
    private LocalDate disbursementDate;

    @Column(name = "first_emi_date")
    private LocalDate firstEmiDate;

    // State machine
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private LoanStatus status;

    @Column(name = "rejection_reasons", columnDefinition = "TEXT")
    private String rejectionReasons;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "disbursed_at")
    private LocalDateTime disbursedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "correlation_id")
    private String correlationId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = "LOAN-" + UUID.randomUUID()
                    .toString().substring(0, 8).toUpperCase();
        }
        if (this.status == null) {
            this.status = LoanStatus.DRAFT;
        }
    }
}