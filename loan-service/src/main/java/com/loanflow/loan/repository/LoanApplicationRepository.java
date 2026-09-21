package com.loanflow.loan.repository;

import com.loanflow.commons.enums.LoanStatus;
import com.loanflow.loan.entity.LoanApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoanApplicationRepository extends JpaRepository<LoanApplication, String> {

    Page<LoanApplication> findByUserId(String userId, Pageable pageable);

    Page<LoanApplication> findByStatus(LoanStatus status, Pageable pageable);

    Optional<LoanApplication> findByIdAndUserId(String id, String userId);

    boolean existsByUserIdAndStatusIn(String userId, List<LoanStatus> statuses);

    @Query("SELECT la FROM LoanApplication la WHERE la.status = 'UNDER_REVIEW' " +
            "AND la.makerOfficerId IS NULL ORDER BY la.submittedAt ASC")
    List<LoanApplication> findUnassignedForReview(Pageable pageable);

    @Query("SELECT la FROM LoanApplication la WHERE la.status = 'CREDIT_CHECK_IN_PROGRESS' " +
            "AND la.updatedAt < :threshold")
    List<LoanApplication> findStaleCreditChecks(LocalDateTime threshold);

    @Query("SELECT la.status, COUNT(la) FROM LoanApplication la GROUP BY la.status")
    List<Object[]> countByStatus();
}