package com.loanflow.credit.controller;

import com.loanflow.commons.dto.ApiResponse;
import com.loanflow.credit.entity.CreditAssessment;
import com.loanflow.credit.repository.CreditAssessmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/credit")
@RequiredArgsConstructor
public class CreditController {

    private final CreditAssessmentRepository assessmentRepository;

    @GetMapping("/assessment/{loanId}")
    public ResponseEntity<ApiResponse<CreditAssessment>> getAssessment(
            @PathVariable String loanId) {

        CreditAssessment assessment = assessmentRepository
                .findByLoanApplicationId(loanId)
                .orElseThrow(() -> new RuntimeException(
                        "Assessment not found for loan: " + loanId));

        return ResponseEntity.ok(ApiResponse.success(assessment));
    }
}