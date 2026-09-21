package com.loanflow.emi.service;

import com.loanflow.commons.utils.EmiCalculator;
import com.loanflow.emi.entity.*;
import com.loanflow.emi.repository.EmiScheduleRepository;
import com.loanflow.emi.repository.LoanAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmiScheduleGenerator {

    private final EmiScheduleRepository emiRepository;
    private final LoanAccountRepository accountRepository;

    private static final int    SCALE   = 2;
    private static final RoundingMode RM = RoundingMode.HALF_UP;

    @Transactional
    public void generateSchedule(
            String loanApplicationId,
            String userId,
            BigDecimal approvedAmount,
            BigDecimal annualInterestRate,
            Integer tenureMonths,
            BigDecimal emiAmount,
            LocalDate firstEmiDate) {

        log.info("Generating EMI schedule for loan {} — ₹{} @ {}% for {} months",
                loanApplicationId, approvedAmount,
                annualInterestRate, tenureMonths);

        BigDecimal monthlyRate = annualInterestRate
                .divide(BigDecimal.valueOf(1200), 10, RM);

        List<EmiSchedule> schedule = new ArrayList<>();
        BigDecimal outstanding  = approvedAmount;
        BigDecimal totalInterest = BigDecimal.ZERO;

        for (int i = 1; i <= tenureMonths; i++) {
            BigDecimal interest  = outstanding.multiply(monthlyRate)
                    .setScale(SCALE, RM);
            BigDecimal principal = emiAmount.subtract(interest)
                    .setScale(SCALE, RM);

            if (i == tenureMonths) {
                principal = outstanding;
                interest  = emiAmount.subtract(principal)
                        .max(BigDecimal.ZERO).setScale(SCALE, RM);
            }

            outstanding    = outstanding.subtract(principal)
                    .max(BigDecimal.ZERO).setScale(SCALE, RM);
            totalInterest  = totalInterest.add(interest);

            schedule.add(EmiSchedule.builder()
                    .loanApplicationId(loanApplicationId)
                    .userId(userId)
                    .emiNumber(i)
                    .totalEmis(tenureMonths)
                    .emiAmount(emiAmount)
                    .principalComponent(principal)
                    .interestComponent(interest)
                    .outstandingBalance(outstanding)
                    .dueDate(firstEmiDate.plusMonths(i - 1))
                    .status(EmiStatus.PENDING)
                    .build());
        }

        emiRepository.saveAll(schedule);
        log.info("Saved {} EMI installments for loan {}",
                schedule.size(), loanApplicationId);

        LoanAccount account = LoanAccount.builder()
                .loanApplicationId(loanApplicationId)
                .userId(userId)
                .principalAmount(approvedAmount)
                .outstandingPrincipal(approvedAmount)
                .totalInterestPayable(totalInterest.setScale(SCALE, RM))
                .totalInterestPaid(BigDecimal.ZERO)
                .totalPrincipalPaid(BigDecimal.ZERO)
                .totalPenaltyCharged(BigDecimal.ZERO)
                .annualInterestRate(annualInterestRate)
                .tenureMonths(tenureMonths)
                .emisPaid(0)
                .emisRemaining(tenureMonths)
                .emiAmount(emiAmount)
                .firstEmiDate(firstEmiDate)
                .lastEmiDate(firstEmiDate.plusMonths(tenureMonths - 1))
                .nextEmiDueDate(firstEmiDate)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        accountRepository.save(account);
        log.info("Created LoanAccount for loan {}", loanApplicationId);
    }

    @Transactional
    public void applyPayment(String loanApplicationId,
                             int emiNumber,
                             BigDecimal amountPaid,
                             String paymentId) {

        EmiSchedule emi = emiRepository
                .findByLoanApplicationIdAndEmiNumber(
                        loanApplicationId, emiNumber)
                .orElseThrow(() -> new RuntimeException(
                        "EMI #" + emiNumber + " not found for loan "
                                + loanApplicationId));

        emi.setAmountPaid(amountPaid);
        emi.setPaidDate(LocalDate.now());
        emi.setPaymentId(paymentId);
        emi.setStatus(amountPaid.compareTo(emi.getEmiAmount()) >= 0
                ? EmiStatus.PAID : EmiStatus.PARTIALLY_PAID);
        emiRepository.save(emi);

        LoanAccount account = accountRepository
                .findByLoanApplicationId(loanApplicationId)
                .orElseThrow();

        account.setTotalPrincipalPaid(
                account.getTotalPrincipalPaid()
                        .add(emi.getPrincipalComponent()));
        account.setTotalInterestPaid(
                account.getTotalInterestPaid()
                        .add(emi.getInterestComponent()));
        account.setOutstandingPrincipal(emi.getOutstandingBalance());
        account.setEmisPaid(account.getEmisPaid() + 1);
        account.setEmisRemaining(account.getEmisRemaining() - 1);

        emiRepository.findNextPending(loanApplicationId)
                .ifPresent(next ->
                        account.setNextEmiDueDate(next.getDueDate()));

        if (account.getEmisRemaining() == 0) {
            account.setAccountStatus(AccountStatus.CLOSED);
            log.info("Loan {} fully repaid — account CLOSED",
                    loanApplicationId);
        }

        accountRepository.save(account);
        log.info("Applied payment ₹{} to EMI #{} for loan {}",
                amountPaid, emiNumber, loanApplicationId);
    }

    public BigDecimal calculatePenalty(BigDecimal emiAmount,
                                       int overdueDays) {
        return emiAmount
                .multiply(BigDecimal.valueOf(0.02))
                .multiply(BigDecimal.valueOf(overdueDays))
                .divide(BigDecimal.valueOf(30), SCALE, RM);
    }
}