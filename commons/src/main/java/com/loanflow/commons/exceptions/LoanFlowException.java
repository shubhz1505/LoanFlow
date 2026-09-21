package com.loanflow.commons.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class LoanFlowException extends RuntimeException {

    private final String     errorCode;
    private final HttpStatus httpStatus;

    public LoanFlowException(String message,
                             String errorCode,
                             HttpStatus httpStatus) {
        super(message);
        this.errorCode  = errorCode;
        this.httpStatus = httpStatus;
    }
}