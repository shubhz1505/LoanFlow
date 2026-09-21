package com.loanflow.emi.repository;

import com.loanflow.emi.entity.EmiSchedule;
import com.loanflow.emi.entity.EmiStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmiScheduleRepository
        extends JpaRepository<EmiSchedule, String> {

    List<EmiSchedule> findByLoanApplicationIdOrderByEmiNumberAsc(
            String loanApplicationId);

    Optional<EmiSchedule> findByLoanApplicationIdAndEmiNumber(
            String loanApplicationId, int emiNumber);

    @Query("SELECT e FROM EmiSchedule e WHERE e.dueDate = :targetDate " +
            "AND e.status = 'PENDING'")
    List<EmiSchedule> findDueOn(LocalDate targetDate);

    @Query("SELECT e FROM EmiSchedule e WHERE e.dueDate < :today " +
            "AND e.status IN ('PENDING', 'DUE', 'PARTIALLY_PAID')")
    List<EmiSchedule> findAllOverdue(LocalDate today);

    @Query("SELECT e FROM EmiSchedule e WHERE e.loanApplicationId = :loanId " +
            "AND e.status = 'PENDING' ORDER BY e.emiNumber ASC LIMIT 1")
    Optional<EmiSchedule> findNextPending(String loanId);

    @Query("SELECT COUNT(e) FROM EmiSchedule e " +
            "WHERE e.loanApplicationId = :loanId AND e.status = 'OVERDUE'")
    int countOverdue(String loanId);
}