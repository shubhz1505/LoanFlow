package com.loanflow.loan.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class MakerDecisionRequest {

    @NotBlank
    private String loanApplicationId;

    @NotBlank
    private String decision;

    @DecimalMin("10000")
    private BigDecimal approvedAmount;

    @DecimalMin("1") @DecimalMax("36")
    private BigDecimal approvedInterestRate;

    private Integer approvedTenureMonths;

    @Size(max = 1000)
    private String remarks;
}