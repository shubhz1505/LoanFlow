package com.loanflow.emi.controller;

import com.loanflow.commons.dto.ApiResponse;
import com.loanflow.emi.entity.EmiSchedule;
import com.loanflow.emi.entity.LoanAccount;
import com.loanflow.emi.repository.EmiScheduleRepository;
import com.loanflow.emi.repository.LoanAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/emi")
@RequiredArgsConstructor
public class EmiController {

    private final EmiScheduleRepository emiRepository;
    private final LoanAccountRepository accountRepository;

    @GetMapping("/loan/{loanId}/schedule")
    public ResponseEntity<ApiResponse<List<EmiSchedule>>> getSchedule(
            @PathVariable String loanId) {

        return ResponseEntity.ok(ApiResponse.success(
                emiRepository.findByLoanApplicationIdOrderByEmiNumberAsc(loanId)));
    }

    @GetMapping("/loan/{loanId}/account")
    public ResponseEntity<ApiResponse<LoanAccount>> getAccount(
            @PathVariable String loanId) {

        LoanAccount account = accountRepository
                .findByLoanApplicationId(loanId)
                .orElseThrow(() -> new RuntimeException(
                        "Account not found: " + loanId));

        return ResponseEntity.ok(ApiResponse.success(account));
    }
}