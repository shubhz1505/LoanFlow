package com.loanflow.credit.entity;

import com.loanflow.commons.enums.EmploymentType;
import com.loanflow.commons.enums.RiskTier;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "credit_assessments",
        indexes = {
                @Index(name = "idx_ca_loan_id",   columnList = "loan_application_id"),
                @Index(name = "idx_ca_user_id",   columnList = "user_id"),
                @Index(name = "idx_ca_risk_tier", columnList = "risk_tier")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreditAssessment {

    @Id
    private String id;

    @Column(name = "loan_application_id", nullable = false, unique = true)
    private String loanApplicationId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "monthly_income", nullable = false, precision = 15, scale = 2)
    private BigDecimal monthlyIncome;

    @Column(name = "requested_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal requestedAmount;

    @Column(name = "tenure_months", nullable = false)
    private Integer tenureMonths;

    @Column(name = "existing_emi_amount", precision = 15, scale = 2)
    private BigDecimal existingEmiAmount;

    @Column(name = "existing_loan_count")
    private Integer existingLoanCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type")
    private EmploymentType employmentType;

    @Column(name = "debt_to_income_ratio", precision = 6, scale = 3)
    private BigDecimal debtToIncomeRatio;

    @Column(name = "credit_score")
    private Integer creditScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_tier")
    private RiskTier riskTier;

    @Column(name = "max_eligible_amount", precision = 15, scale = 2)
    private BigDecimal maxEligibleAmount;

    @Column(name = "recommended_interest_rate", precision = 6, scale = 3)
    private BigDecimal recommendedInterestRate;

    @Column(name = "rejection_reasons", columnDefinition = "TEXT")
    private String rejectionReasons;

    @Column(name = "shap_explanation", columnDefinition = "TEXT")
    private String shapExplanation;

    @Column(name = "is_manual_review")
    private Boolean isManualReview;

    @Column(name = "ml_engine_version")
    private String mlEngineVersion;

    @Column(name = "scoring_duration_ms")
    private Long scoringDurationMs;

    @Column(name = "correlation_id")
    private String correlationId;

    @CreationTimestamp
    @Column(name = "scored_at", updatable = false)
    private LocalDateTime scoredAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
    }
}