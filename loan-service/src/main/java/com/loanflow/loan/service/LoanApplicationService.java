package com.loanflow.loan.service;

import com.loanflow.commons.enums.LoanStatus;
import com.loanflow.commons.events.LoanApplicationSubmittedEvent;
import com.loanflow.commons.events.LoanApprovedEvent;
import com.loanflow.commons.events.LoanRejectedEvent;
import com.loanflow.commons.exceptions.InvalidStateException;
import com.loanflow.commons.exceptions.ResourceNotFoundException;
import com.loanflow.commons.exceptions.UnauthorizedException;
import com.loanflow.commons.utils.CorrelationIdUtils;
import com.loanflow.commons.utils.EmiCalculator;
import com.loanflow.loan.dto.request.CheckerDecisionRequest;
import com.loanflow.loan.dto.request.LoanApplicationRequest;
import com.loanflow.loan.dto.request.MakerDecisionRequest;
import com.loanflow.loan.dto.response.LoanApplicationResponse;
import com.loanflow.loan.entity.LoanApplication;
import com.loanflow.loan.kafka.LoanEventPublisher;
import com.loanflow.loan.repository.LoanApplicationRepository;
import com.loanflow.loan.repository.LoanStateHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanApplicationService {

    private final LoanApplicationRepository  loanRepository;
    private final LoanStateHistoryRepository historyRepository;
    private final LoanStateTransitionService transitionService;
    private final LoanEventPublisher         eventPublisher;

    private static final List<LoanStatus> ACTIVE_STATUSES = List.of(
            LoanStatus.SUBMITTED, LoanStatus.DOCUMENT_PENDING,
            LoanStatus.CREDIT_CHECK_IN_PROGRESS, LoanStatus.UNDER_REVIEW,
            LoanStatus.CONDITIONALLY_APPROVED, LoanStatus.APPROVED,
            LoanStatus.DISBURSEMENT_PENDING, LoanStatus.DISBURSED,
            LoanStatus.ACTIVE);

    @Transactional
    public LoanApplicationResponse submitApplication(
            String userId, String userEmail, String userFullName,
            LoanApplicationRequest request) {

        if (loanRepository.existsByUserIdAndStatusIn(userId, ACTIVE_STATUSES)) {
            throw new InvalidStateException(
                    "You already have an active loan application.");
        }

        String correlationId = CorrelationIdUtils.generateWithPrefix("LOAN");

        LoanApplication loan = LoanApplication.builder()
                .userId(userId)
                .userEmail(userEmail)
                .userFullName(userFullName)
                .loanType(request.getLoanType())
                .requestedAmount(request.getRequestedAmount())
                .tenureMonths(request.getTenureMonths())
                .monthlyIncome(request.getMonthlyIncome())
                .employmentType(request.getEmploymentType())
                .existingEmiAmount(request.getExistingEmiAmount())
                .existingLoanCount(request.getExistingLoanCount())
                .loanPurpose(request.getLoanPurpose())
                .status(LoanStatus.DRAFT)
                .correlationId(correlationId)
                .build();

        loanRepository.save(loan);

        transitionService.transitionTo(loan, LoanStatus.SUBMITTED,
                userId, "APPLICANT", "Application submitted by user");

        eventPublisher.publishSubmitted(LoanApplicationSubmittedEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .correlationId(correlationId)
                .occurredAt(LocalDateTime.now())
                .sourceService("loan-service")
                .eventVersion("1.0")
                .loanApplicationId(loan.getId())
                .userId(userId)
                .userEmail(userEmail)
                .userFullName(userFullName)
                .loanType(loan.getLoanType())
                .requestedAmount(loan.getRequestedAmount())
                .tenureMonths(loan.getTenureMonths())
                .monthlyIncome(loan.getMonthlyIncome())
                .employmentType(loan.getEmploymentType())
                .existingEmiAmount(loan.getExistingEmiAmount())
                .existingLoanCount(loan.getExistingLoanCount())
                .loanPurpose(loan.getLoanPurpose())
                .submittedAt(LocalDateTime.now())
                .build());

        transitionService.transitionTo(loan, LoanStatus.DOCUMENT_PENDING,
                "SYSTEM", "SYSTEM", "Waiting for document verification");

        log.info("[{}] Loan {} submitted by user {}", correlationId, loan.getId(), userId);
        return mapToResponse(loan);
    }

    @Transactional
    public LoanApplicationResponse makerDecision(String officerId,
                                                 MakerDecisionRequest request) {
        LoanApplication loan = getLoan(request.getLoanApplicationId());

        if (loan.getStatus() != LoanStatus.UNDER_REVIEW) {
            throw new InvalidStateException(
                    "Loan not in UNDER_REVIEW. Current: " + loan.getStatus());
        }

        loan.setMakerOfficerId(officerId);
        loan.setMakerDecision(request.getDecision());
        loan.setMakerRemarks(request.getRemarks());
        loan.setMakerDecisionAt(LocalDateTime.now());

        if ("RECOMMEND_APPROVE".equals(request.getDecision())) {
            loan.setApprovedAmount(request.getApprovedAmount());
            loan.setApprovedInterestRate(request.getApprovedInterestRate());
            loan.setApprovedTenureMonths(request.getApprovedTenureMonths());
            loan.setEmiAmount(EmiCalculator.calculateEmi(
                    request.getApprovedAmount(),
                    request.getApprovedInterestRate(),
                    request.getApprovedTenureMonths()));
            transitionService.transitionTo(loan, LoanStatus.CONDITIONALLY_APPROVED,
                    officerId, "LOAN_OFFICER",
                    "Maker recommends approval: " + request.getRemarks());
        } else {
            transitionService.transitionTo(loan, LoanStatus.REJECTED,
                    officerId, "LOAN_OFFICER",
                    "Maker recommends rejection: " + request.getRemarks());
            publishRejected(loan, List.of(request.getRemarks()));
        }

        return mapToResponse(loan);
    }

    @Transactional
    public LoanApplicationResponse checkerDecision(String officerId,
                                                   String officerRole,
                                                   CheckerDecisionRequest request) {
        LoanApplication loan = getLoan(request.getLoanApplicationId());

        if (loan.getStatus() != LoanStatus.CONDITIONALLY_APPROVED) {
            throw new InvalidStateException(
                    "Loan not in CONDITIONALLY_APPROVED. Current: " + loan.getStatus());
        }

        if (officerId.equals(loan.getMakerOfficerId())) {
            throw new UnauthorizedException(
                    "Maker and Checker must be different officers.");
        }

        loan.setCheckerOfficerId(officerId);
        loan.setCheckerDecisionAt(LocalDateTime.now());
        loan.setCheckerRemarks(request.getRemarks());
        loanRepository.save(loan);

        if (request.getDecision() == LoanStatus.APPROVED) {
            transitionService.transitionTo(loan, LoanStatus.APPROVED,
                    officerId, officerRole,
                    "Checker approved: " + request.getRemarks());
            transitionService.transitionTo(loan, LoanStatus.DISBURSEMENT_PENDING,
                    "SYSTEM", "SYSTEM", "Queued for disbursement");

            loan.setDisbursementDate(LocalDate.now().plusDays(2));
            loan.setFirstEmiDate(LocalDate.now().plusDays(32));
            loanRepository.save(loan);

            publishApproved(loan);

        } else if (request.getDecision() == LoanStatus.REJECTED) {
            transitionService.transitionTo(loan, LoanStatus.REJECTED,
                    officerId, officerRole,
                    "Checker rejected: " + request.getRemarks());
            publishRejected(loan, List.of(request.getRemarks()));
        }

        return mapToResponse(loan);
    }

    @Transactional(readOnly = true)
    public LoanApplicationResponse getApplication(String loanId,
                                                  String userId,
                                                  String role) {
        LoanApplication loan = "APPLICANT".equals(role)
                ? loanRepository.findByIdAndUserId(loanId, userId)
                  .orElseThrow(() -> new ResourceNotFoundException(
                          "LoanApplication", loanId))
                : getLoan(loanId);
        return mapToResponse(loan);
    }

    @Transactional(readOnly = true)
    public Page<LoanApplicationResponse> getMyApplications(String userId,
                                                           Pageable pageable) {
        return loanRepository.findByUserId(userId, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<LoanApplicationResponse> getApplicationsForReview(Pageable pageable) {
        return loanRepository.findByStatus(LoanStatus.UNDER_REVIEW, pageable)
                .map(this::mapToResponse);
    }

    private void publishApproved(LoanApplication loan) {
        eventPublisher.publishApproved(LoanApprovedEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .correlationId(loan.getCorrelationId())
                .occurredAt(LocalDateTime.now())
                .sourceService("loan-service")
                .eventVersion("1.0")
                .loanApplicationId(loan.getId())
                .userId(loan.getUserId())
                .userEmail(loan.getUserEmail())
                .loanType(loan.getLoanType())
                .approvedAmount(loan.getApprovedAmount())
                .interestRate(loan.getApprovedInterestRate())
                .tenureMonths(loan.getApprovedTenureMonths())
                .emiAmount(loan.getEmiAmount())
                .disbursementDate(loan.getDisbursementDate())
                .approvedByOfficerId(loan.getMakerOfficerId())
                .checkedByOfficerId(loan.getCheckerOfficerId())
                .approvedAt(loan.getApprovedAt())
                .build());
    }

    private void publishRejected(LoanApplication loan, List<String> reasons) {
        eventPublisher.publishRejected(LoanRejectedEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .correlationId(loan.getCorrelationId())
                .occurredAt(LocalDateTime.now())
                .sourceService("loan-service")
                .eventVersion("1.0")
                .loanApplicationId(loan.getId())
                .userId(loan.getUserId())
                .userEmail(loan.getUserEmail())
                .rejectionReasons(reasons)
                .rejectedByOfficerId(loan.getMakerOfficerId())
                .rejectedAt(loan.getRejectedAt())
                .build());
    }

    private LoanApplicationResponse mapToResponse(LoanApplication loan) {
        LoanApplicationResponse resp = new LoanApplicationResponse();
        resp.setId(loan.getId());
        resp.setUserId(loan.getUserId());
        resp.setUserFullName(loan.getUserFullName());
        resp.setLoanType(loan.getLoanType());
        resp.setRequestedAmount(loan.getRequestedAmount());
        resp.setTenureMonths(loan.getTenureMonths());
        resp.setMonthlyIncome(loan.getMonthlyIncome());
        resp.setStatus(loan.getStatus());
        resp.setCreditScore(loan.getCreditScore());
        resp.setRiskTier(loan.getRiskTier());
        resp.setApprovedAmount(loan.getApprovedAmount());
        resp.setApprovedInterestRate(loan.getApprovedInterestRate());
        resp.setEmiAmount(loan.getEmiAmount());
        resp.setDisbursementDate(loan.getDisbursementDate());
        resp.setMakerDecision(loan.getMakerDecision());
        resp.setRejectionReasons(loan.getRejectionReasons());
        resp.setSubmittedAt(loan.getSubmittedAt());
        resp.setCreatedAt(loan.getCreatedAt());
        resp.setStateHistory(historyRepository
                .findByLoanApplicationIdOrderByTransitionedAtAsc(loan.getId())
                .stream()
                .map(h -> {
                    var s = new com.loanflow.loan.dto.response.StateHistoryResponse();
                    s.setFromStatus(h.getFromStatus());
                    s.setToStatus(h.getToStatus());
                    s.setPerformedBy(h.getPerformedBy());
                    s.setRemarks(h.getRemarks());
                    s.setTransitionedAt(h.getTransitionedAt());
                    return s;
                }).collect(Collectors.toList()));
        return resp;
    }

    private LoanApplication getLoan(String loanId) {
        return loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "LoanApplication", loanId));
    }
}