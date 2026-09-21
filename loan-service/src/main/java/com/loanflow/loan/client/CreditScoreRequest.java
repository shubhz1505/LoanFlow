package com.loanflow.loan.client;

import com.loanflow.commons.enums.EmploymentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditScoreRequest {
    private String         loanApplicationId;
    private String         userId;
    private BigDecimal     monthlyIncome;
    private BigDecimal     requestedAmount;
    private Integer        tenureMonths;
    private BigDecimal     existingEmiAmount;
    private Integer        existingLoanCount;
    private EmploymentType employmentType;
    private String         correlationId;
}