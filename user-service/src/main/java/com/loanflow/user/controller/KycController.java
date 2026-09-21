package com.loanflow.user.controller;

import com.loanflow.commons.dto.ApiResponse;
import com.loanflow.user.dto.request.KycReviewRequest;
import com.loanflow.user.dto.request.KycSubmitRequest;
import com.loanflow.user.entity.KycAuditLog;
import com.loanflow.user.service.KycService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/kyc")
@RequiredArgsConstructor
public class KycController {

    private final KycService kycService;

    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<Void>> submitKyc(
            @Valid @RequestBody KycSubmitRequest request,
            @RequestHeader("X-User-Id") String userId) {

        kycService.submitKyc(userId, request);
        return ResponseEntity.ok(ApiResponse.success(null,
                "KYC submitted. Under review by our team."));
    }

    @PostMapping("/review")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER','SENIOR_OFFICER','ADMIN')")
    public ResponseEntity<ApiResponse<Void>> reviewKyc(
            @Valid @RequestBody KycReviewRequest request,
            @RequestHeader("X-User-Id") String officerId,
            @RequestHeader("X-User-Role") String officerRole,
            HttpServletRequest httpRequest) {

        kycService.reviewKyc(officerId, officerRole, request,
                httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success(null, "KYC review submitted"));
    }

    @GetMapping("/history/{userId}")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER','SENIOR_OFFICER','ADMIN')")
    public ResponseEntity<ApiResponse<List<KycAuditLog>>> kycHistory(
            @PathVariable String userId) {

        return ResponseEntity.ok(ApiResponse.success(
                kycService.getKycHistory(userId)));
    }
}