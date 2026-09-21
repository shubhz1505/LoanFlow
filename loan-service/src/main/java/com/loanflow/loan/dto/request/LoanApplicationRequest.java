package com.loanflow.loan.dto.request;

import com.loanflow.commons.enums.EmploymentType;
import com.loanflow.commons.enums.LoanType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LoanApplicationRequest {

    @NotNull
    private LoanType loanType;

    @NotNull @DecimalMin("10000") @DecimalMax("10000000")
    private BigDecimal requestedAmount;

    @NotNull @Min(6) @Max(360)
    private Integer tenureMonths;

    @NotNull @DecimalMin("5000")
    private BigDecimal monthlyIncome;

    @NotNull
    private EmploymentType employmentType;

    @DecimalMin("0")
    private BigDecimal existingEmiAmount;

    @Min(0) @Max(20)
    private Integer existingLoanCount;

    @Size(max = 500)
    private String loanPurpose;
}