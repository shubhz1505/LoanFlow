package com.loanflow.commons.exceptions;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends LoanFlowException {
    public ResourceNotFoundException(String resource, String id) {
        super(resource + " not found: " + id,
                "RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}