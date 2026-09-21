package com.loanflow.emi.scheduler;

import com.loanflow.commons.events.EmiDueReminderEvent;
import com.loanflow.commons.events.PaymentOverdueEvent;
import com.loanflow.commons.kafka.EventPublisher;
import com.loanflow.emi.entity.*;
import com.loanflow.emi.repository.EmiScheduleRepository;
import com.loanflow.emi.repository.LoanAccountRepository;
import com.loanflow.emi.service.EmiScheduleGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmiSchedulerJob {

    private final EmiScheduleRepository  emiRepository;
    private final LoanAccountRepository  accountRepository;
    private final EventPublisher         eventPublisher;
    private final EmiScheduleGenerator   generator;

    private static final int NPA_THRESHOLD_DAYS    = 90;
    private static final int REMINDER_DAYS_BEFORE  = 3;

    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void detectOverdueEmis() {
        LocalDate today = LocalDate.now();
        List<EmiSchedule> overdueList = emiRepository.findAllOverdue(today);

        log.info("Overdue detection: found {} overdue EMIs", overdueList.size());

        for (EmiSchedule emi : overdueList) {
            int overdueDays = (int) ChronoUnit.DAYS.between(
                    emi.getDueDate(), today);
            BigDecimal penalty = generator.calculatePenalty(
                    emi.getEmiAmount(), overdueDays);

            emi.setStatus(EmiStatus.OVERDUE);
            emi.setOverdueDays(overdueDays);
            emi.setPenaltyAmount(penalty);
            emiRepository.save(emi);

            LoanAccount account = accountRepository
                    .findByLoanApplicationId(emi.getLoanApplicationId())
                    .orElse(null);

            if (account != null) {
                account.setTotalPenaltyCharged(
                        account.getTotalPenaltyCharged().add(penalty));
                if (overdueDays >= NPA_THRESHOLD_DAYS) {
                    account.setAccountStatus(AccountStatus.NPA);
                    log.warn("Loan {} marked NPA — {} days overdue",
                            emi.getLoanApplicationId(), overdueDays);
                } else {
                    account.setAccountStatus(AccountStatus.OVERDUE);
                }
                accountRepository.save(account);
            }

            PaymentOverdueEvent event = PaymentOverdueEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .correlationId(UUID.randomUUID().toString())
                    .occurredAt(LocalDateTime.now())
                    .sourceService("emi-service")
                    .eventVersion("1.0")
                    .loanApplicationId(emi.getLoanApplicationId())
                    .userId(emi.getUserId())
                    .overdueDays(overdueDays)
                    .overdueAmount(emi.getEmiAmount())
                    .penaltyAmount(penalty)
                    .emiNumber(emi.getEmiNumber())
                    .dueDate(emi.getDueDate())
                    .detectedAt(LocalDateTime.now())
                    .build();

            eventPublisher.publish("payment.overdue",
                    emi.getLoanApplicationId(), event,
                    event.getCorrelationId());
        }

        log.info("Overdue detection complete — {} EMIs processed",
                overdueList.size());
    }

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void sendEmiReminders() {
        LocalDate targetDate = LocalDate.now().plusDays(REMINDER_DAYS_BEFORE);
        List<EmiSchedule> upcoming = emiRepository.findDueOn(targetDate);

        log.info("Reminder job: {} EMIs due on {}", upcoming.size(), targetDate);

        for (EmiSchedule emi : upcoming) {
            if (Boolean.TRUE.equals(emi.getReminderSent())) continue;

            LoanAccount account = accountRepository
                    .findByLoanApplicationId(emi.getLoanApplicationId())
                    .orElse(null);

            EmiDueReminderEvent event = EmiDueReminderEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .correlationId(UUID.randomUUID().toString())
                    .occurredAt(LocalDateTime.now())
                    .sourceService("emi-service")
                    .eventVersion("1.0")
                    .loanApplicationId(emi.getLoanApplicationId())
                    .userId(emi.getUserId())
                    .emiAmount(emi.getEmiAmount())
                    .dueDate(emi.getDueDate())
                    .daysUntilDue(REMINDER_DAYS_BEFORE)
                    .emiNumber(emi.getEmiNumber())
                    .outstandingBalance(account != null
                            ? account.getOutstandingPrincipal()
                            : BigDecimal.ZERO)
                    .triggeredAt(LocalDateTime.now())
                    .build();

            eventPublisher.publish("emi.due.reminder",
                    emi.getLoanApplicationId(), event,
                    event.getCorrelationId());

            emi.setReminderSent(true);
            emi.setReminderSentAt(LocalDateTime.now());
            emiRepository.save(emi);
        }

        log.info("Reminder job complete — {} reminders sent", upcoming.size());
    }
}