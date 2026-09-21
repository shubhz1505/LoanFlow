package com.loanflow.loan.controller;

import com.loanflow.commons.dto.ApiResponse;
import com.loanflow.loan.dto.request.CheckerDecisionRequest;
import com.loanflow.loan.dto.request.LoanApplicationRequest;
import com.loanflow.loan.dto.request.MakerDecisionRequest;
import com.loanflow.loan.dto.response.LoanApplicationResponse;
import com.loanflow.loan.service.LoanApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanApplicationService loanService;

    @PostMapping
    public ResponseEntity<ApiResponse<LoanApplicationResponse>> submit(
            @Valid @RequestBody LoanApplicationRequest request,
            @RequestHeader("X-User-Id")    String userId,
            @RequestHeader("X-User-Email") String userEmail,
            @RequestHeader(value = "X-User-Name", defaultValue = "") String userName) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        loanService.submitApplication(userId, userEmail, userName, request),
                        "Loan application submitted successfully"));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<Page<LoanApplicationResponse>>> myApplications(
            @RequestHeader("X-User-Id") String userId,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.success(
                loanService.getMyApplications(userId, pageable)));
    }

    @GetMapping("/{loanId}")
    public ResponseEntity<ApiResponse<LoanApplicationResponse>> getApplication(
            @PathVariable String loanId,
            @RequestHeader("X-User-Id")   String userId,
            @RequestHeader("X-User-Role") String role) {

        return ResponseEntity.ok(ApiResponse.success(
                loanService.getApplication(loanId, userId, role)));
    }

    @GetMapping("/review/queue")
    public ResponseEntity<ApiResponse<Page<LoanApplicationResponse>>> reviewQueue(
            @PageableDefault(size = 20, sort = "submittedAt") Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.success(
                loanService.getApplicationsForReview(pageable)));
    }

    @PostMapping("/review/maker-decision")
    public ResponseEntity<ApiResponse<LoanApplicationResponse>> makerDecision(
            @Valid @RequestBody MakerDecisionRequest request,
            @RequestHeader("X-User-Id") String officerId) {

        return ResponseEntity.ok(ApiResponse.success(
                loanService.makerDecision(officerId, request),
                "Maker decision recorded"));
    }

    @PostMapping("/review/checker-decision")
    public ResponseEntity<ApiResponse<LoanApplicationResponse>> checkerDecision(
            @Valid @RequestBody CheckerDecisionRequest request,
            @RequestHeader("X-User-Id")   String officerId,
            @RequestHeader("X-User-Role") String officerRole) {

        return ResponseEntity.ok(ApiResponse.success(
                loanService.checkerDecision(officerId, officerRole, request),
                "Checker decision recorded"));
    }
}