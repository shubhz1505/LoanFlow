package com.loanflow.loan.client;

import com.loanflow.commons.enums.RiskTier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditScoreResponse {
    private Integer            creditScore;
    private RiskTier           riskTier;
    private BigDecimal         maxEligibleAmount;
    private BigDecimal         recommendedInterestRate;
    private List<String>       rejectionReasons;
    private Boolean            isManualReview;
    private Map<String, Double> shapExplanation;
}