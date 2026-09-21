package com.loanflow.commons.exceptions;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends LoanFlowException {

    public UnauthorizedException(String message) {
        super(message, "UNAUTHORIZED", HttpStatus.FORBIDDEN);
    }
}