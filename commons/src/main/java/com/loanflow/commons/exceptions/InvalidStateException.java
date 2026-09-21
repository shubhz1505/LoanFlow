package com.loanflow.commons.exceptions;

import org.springframework.http.HttpStatus;

public class InvalidStateException extends LoanFlowException {

    public InvalidStateException(String message) {
        super(message, "INVALID_STATE", HttpStatus.CONFLICT);
    }
}