package com.loanflow.loan.dto.response;

import com.loanflow.commons.enums.LoanStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StateHistoryResponse {
    private LoanStatus    fromStatus;
    private LoanStatus    toStatus;
    private String        performedBy;
    private String        remarks;
    private LocalDateTime transitionedAt;
}