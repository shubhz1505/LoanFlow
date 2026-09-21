package com.loanflow.payment.service;

import com.loanflow.commons.events.PaymentReceivedEvent;
import com.loanflow.commons.kafka.EventPublisher;
import com.loanflow.commons.utils.CorrelationIdUtils;
import com.loanflow.payment.entity.*;
import com.loanflow.payment.repository.LedgerEntryRepository;
import com.loanflow.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository      paymentRepository;
    private final LedgerEntryRepository  ledgerRepository;
    private final EventPublisher         eventPublisher;

    private static final String TOPIC_PAYMENT_RECEIVED = "payment.received";

    @Transactional
    public Payment processPayment(
            String loanApplicationId,
            String userId,
            String userEmail,
            Integer emiNumber,
            BigDecimal amount,
            BigDecimal principalComponent,
            BigDecimal interestComponent,
            BigDecimal penaltyComponent,
            BigDecimal outstandingBalanceAfter,
            PaymentMode paymentMode,
            String transactionReference,
            String correlationId) {

        if (transactionReference != null
                && paymentRepository.existsByTransactionReference(
                transactionReference)) {
            throw new IllegalStateException(
                    "Duplicate transaction reference: " + transactionReference);
        }

        String corrId = correlationId != null
                ? correlationId : CorrelationIdUtils.generate();

        String txnRef = transactionReference != null
                ? transactionReference
                : "LF-" + UUID.randomUUID().toString()
                          .substring(0, 12).toUpperCase();

        Payment payment = Payment.builder()
                .loanApplicationId(loanApplicationId)
                .userId(userId)
                .userEmail(userEmail)
                .amount(amount)
                .principalComponent(principalComponent)
                .interestComponent(interestComponent)
                .penaltyComponent(penaltyComponent != null
                        ? penaltyComponent : BigDecimal.ZERO)
                .outstandingBalanceAfter(outstandingBalanceAfter)
                .emiNumber(emiNumber)
                .paymentMode(paymentMode)
                .status(PaymentStatus.SUCCESS)
                .transactionReference(txnRef)
                .processedAt(LocalDateTime.now())
                .correlationId(corrId)
                .build();

        paymentRepository.save(payment);

        postLedgerEntries(payment);

        PaymentReceivedEvent event = PaymentReceivedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .correlationId(corrId)
                .occurredAt(LocalDateTime.now())
                .sourceService("payment-service")
                .eventVersion("1.0")
                .paymentId(payment.getId())
                .loanApplicationId(loanApplicationId)
                .userId(userId)
                .userEmail(userEmail)
                .amountPaid(amount)
                .principalComponent(principalComponent)
                .interestComponent(interestComponent)
                .outstandingBalance(outstandingBalanceAfter)
                .emiNumber(emiNumber)
                .paymentMode(paymentMode.name())
                .transactionReference(txnRef)
                .paidAt(payment.getProcessedAt())
                .build();

        eventPublisher.publish(TOPIC_PAYMENT_RECEIVED,
                loanApplicationId, event, corrId);

        log.info("[{}] Payment {} processed — ₹{} for loan {} EMI #{}",
                corrId, payment.getId(), amount,
                loanApplicationId, emiNumber);

        return payment;
    }

    private void postLedgerEntries(Payment payment) {
        BigDecimal runningBalance = ledgerRepository
                .getLatestBalanceForLoan(payment.getLoanApplicationId())
                .orElse(payment.getOutstandingBalanceAfter()
                        .add(payment.getAmount()));

        List<LedgerEntry> entries = new ArrayList<>();

        entries.add(LedgerEntry.builder()
                .paymentId(payment.getId())
                .loanApplicationId(payment.getLoanApplicationId())
                .userId(payment.getUserId())
                .entryType(LedgerEntry.EntryType.DEBIT)
                .account(LedgerEntry.LedgerAccount.BORROWER_ACCOUNT)
                .amount(payment.getAmount())
                .runningBalance(runningBalance.subtract(payment.getAmount()))
                .description("EMI #" + payment.getEmiNumber() + " payment")
                .correlationId(payment.getCorrelationId())
                .build());

        entries.add(LedgerEntry.builder()
                .paymentId(payment.getId())
                .loanApplicationId(payment.getLoanApplicationId())
                .userId(payment.getUserId())
                .entryType(LedgerEntry.EntryType.CREDIT)
                .account(LedgerEntry.LedgerAccount.PRINCIPAL_RECEIVED)
                .amount(payment.getPrincipalComponent())
                .runningBalance(payment.getPrincipalComponent())
                .description("Principal — EMI #" + payment.getEmiNumber())
                .correlationId(payment.getCorrelationId())
                .build());

        entries.add(LedgerEntry.builder()
                .paymentId(payment.getId())
                .loanApplicationId(payment.getLoanApplicationId())
                .userId(payment.getUserId())
                .entryType(LedgerEntry.EntryType.CREDIT)
                .account(LedgerEntry.LedgerAccount.INTEREST_RECEIVED)
                .amount(payment.getInterestComponent())
                .runningBalance(payment.getInterestComponent())
                .description("Interest — EMI #" + payment.getEmiNumber())
                .correlationId(payment.getCorrelationId())
                .build());

        if (payment.getPenaltyComponent()
                .compareTo(BigDecimal.ZERO) > 0) {
            entries.add(LedgerEntry.builder()
                    .paymentId(payment.getId())
                    .loanApplicationId(payment.getLoanApplicationId())
                    .userId(payment.getUserId())
                    .entryType(LedgerEntry.EntryType.CREDIT)
                    .account(LedgerEntry.LedgerAccount.PENALTY_RECEIVED)
                    .amount(payment.getPenaltyComponent())
                    .runningBalance(payment.getPenaltyComponent())
                    .description("Penalty — EMI #" + payment.getEmiNumber())
                    .correlationId(payment.getCorrelationId())
                    .build());
        }

        ledgerRepository.saveAll(entries);
        log.debug("Posted {} ledger entries for payment {}",
                entries.size(), payment.getId());
    }
}