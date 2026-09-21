package com.loanflow.payment.controller;

import com.loanflow.commons.dto.ApiResponse;
import com.loanflow.payment.entity.Payment;
import com.loanflow.payment.entity.PaymentMode;
import com.loanflow.payment.repository.LedgerEntryRepository;
import com.loanflow.payment.repository.PaymentRepository;
import com.loanflow.payment.service.PaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService         paymentService;
    private final PaymentRepository      paymentRepository;
    private final LedgerEntryRepository  ledgerRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<Payment>> pay(
            @Valid @RequestBody PaymentRequest request,
            @RequestHeader("X-User-Id")    String userId,
            @RequestHeader("X-User-Email") String userEmail,
            @RequestHeader(value = "X-Correlation-ID",
                    required = false) String correlationId) {

        Payment payment = paymentService.processPayment(
                request.loanApplicationId, userId, userEmail,
                request.emiNumber, request.amount,
                request.principalComponent, request.interestComponent,
                request.penaltyComponent, request.outstandingBalanceAfter,
                request.paymentMode, request.transactionReference,
                correlationId);

        return ResponseEntity.ok(
                ApiResponse.success(payment, "Payment processed successfully"));
    }

    @GetMapping("/loan/{loanId}")
    public ResponseEntity<ApiResponse<List<Payment>>> paymentHistory(
            @PathVariable String loanId) {

        return ResponseEntity.ok(ApiResponse.success(
                paymentRepository
                        .findByLoanApplicationIdOrderByCreatedAtDesc(loanId)));
    }

    @GetMapping("/ledger/{loanId}")
    public ResponseEntity<ApiResponse<?>> ledger(
            @PathVariable String loanId) {

        return ResponseEntity.ok(ApiResponse.success(
                ledgerRepository
                        .findByLoanApplicationIdOrderByCreatedAtAsc(loanId)));
    }

    @GetMapping("/ledger/{loanId}/reconcile")
    public ResponseEntity<ApiResponse<?>> reconcile(
            @PathVariable String loanId) {

        return ResponseEntity.ok(ApiResponse.success(
                ledgerRepository.getLedgerSummary(loanId)));
    }

    @Data
    static class PaymentRequest {
        @NotBlank  public String     loanApplicationId;
        @NotNull   public Integer    emiNumber;
        @NotNull @DecimalMin("1")
        public BigDecimal amount;
        @NotNull   public BigDecimal principalComponent;
        @NotNull   public BigDecimal interestComponent;
        public BigDecimal penaltyComponent;
        @NotNull   public BigDecimal outstandingBalanceAfter;
        @NotNull   public PaymentMode paymentMode;
        public String     transactionReference;
    }
}