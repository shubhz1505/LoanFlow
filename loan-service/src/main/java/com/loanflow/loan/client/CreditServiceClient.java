package com.loanflow.loan.client;

import com.loanflow.commons.dto.ApiResponse;
import com.loanflow.commons.enums.RiskTier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.util.List;

@FeignClient(
        name = "credit-service",
        fallback = CreditServiceClient.CreditServiceFallback.class
)
public interface CreditServiceClient {

    @PostMapping("/api/v1/credit/score")
    ApiResponse<CreditScoreResponse> getCreditScore(
            @RequestBody CreditScoreRequest request);

    @GetMapping("/api/v1/documents/loan/{loanId}/complete")
    ApiResponse<Boolean> areDocumentsComplete(@PathVariable String loanId);

    @Slf4j
    @Component
    class CreditServiceFallback implements CreditServiceClient {

        @Override
        public ApiResponse<CreditScoreResponse> getCreditScore(
                CreditScoreRequest request) {
            log.warn("Credit service unavailable for loan {} — manual review",
                    request.getLoanApplicationId());
            CreditScoreResponse fallback = CreditScoreResponse.builder()
                    .creditScore(0)
                    .riskTier(RiskTier.MANUAL_REVIEW)
                    .maxEligibleAmount(BigDecimal.ZERO)
                    .recommendedInterestRate(BigDecimal.ZERO)
                    .isManualReview(true)
                    .rejectionReasons(List.of())
                    .build();
            return ApiResponse.success(fallback,
                    "Credit service unavailable — manual review");
        }

        @Override
        public ApiResponse<Boolean> areDocumentsComplete(String loanId) {
            log.warn("Document service unavailable for loan {}", loanId);
            return ApiResponse.success(false, "Document service unavailable");
        }
    }
}