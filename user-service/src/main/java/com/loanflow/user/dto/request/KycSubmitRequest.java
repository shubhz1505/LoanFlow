package com.loanflow.user.dto.request;

import com.loanflow.commons.enums.EmploymentType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class KycSubmitRequest {

    @NotBlank
    @Pattern(regexp = "^[A-Z]{5}[0-9]{4}[A-Z]$", message = "Invalid PAN format")
    private String panNumber;

    @NotBlank
    @Pattern(regexp = "^\\d{12}$", message = "Aadhaar must be 12 digits")
    private String aadhaarNumber;

    @NotNull
    private EmploymentType employmentType;

    @NotNull
    @DecimalMin("5000")
    private BigDecimal monthlyIncome;

    private String employerName;
    private String employerAddress;
}