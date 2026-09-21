package com.loanflow.credit.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MlEngineClient {

    private final RestTemplate restTemplate;

    @Value("${ml.engine.base-url}")
    private String mlEngineBaseUrl;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MlScoreRequest {
        private String loanApplicationId;
        private double monthlyIncome;
        private double requestedAmount;
        private int    tenureMonths;
        private double existingEmiAmount;
        private int    existingLoanCount;
        private String employmentType;
        private double debtToIncomeRatio;
        private double creditUtilization;
        private String correlationId;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MlScoreResponse {
        private int                 creditScore;
        private String              riskTier;
        private double              maxEligibleAmount;
        private double              recommendedInterestRate;
        private List<String>        rejectionReasons;
        private Map<String, Double> shapExplanation;
        private String              modelVersion;
        private long                scoringDurationMs;
    }

    @CircuitBreaker(name = "mlEngine", fallbackMethod = "scoreFallback")
    @Retry(name = "mlEngine")
    public MlScoreResponse score(MlScoreRequest request) {
        log.debug("Calling ML engine for loan {}", request.getLoanApplicationId());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Correlation-ID", request.getCorrelationId());

        long start = System.currentTimeMillis();

        ResponseEntity<MlScoreResponse> response = restTemplate.exchange(
                mlEngineBaseUrl + "/score",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                MlScoreResponse.class);

        long duration = System.currentTimeMillis() - start;
        log.info("ML engine responded in {}ms for loan {}",
                duration, request.getLoanApplicationId());

        MlScoreResponse body = response.getBody();
        if (body != null) body.setScoringDurationMs(duration);
        return body;
    }

    public MlScoreResponse scoreFallback(MlScoreRequest request, Throwable ex) {
        log.warn("ML engine unavailable for loan {}. Reason: {}. Manual review.",
                request.getLoanApplicationId(), ex.getMessage());
        return MlScoreResponse.builder()
                .creditScore(0)
                .riskTier("MANUAL_REVIEW")
                .maxEligibleAmount(0)
                .recommendedInterestRate(0)
                .rejectionReasons(List.of())
                .shapExplanation(Map.of())
                .modelVersion("FALLBACK")
                .scoringDurationMs(0)
                .build();
    }
}