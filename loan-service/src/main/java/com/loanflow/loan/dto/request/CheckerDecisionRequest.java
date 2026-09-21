package com.loanflow.loan.dto.request;

import com.loanflow.commons.enums.LoanStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CheckerDecisionRequest {

    @NotBlank
    private String loanApplicationId;

    @NotNull
    private LoanStatus decision;

    @Size(max = 1000)
    private String remarks;
}