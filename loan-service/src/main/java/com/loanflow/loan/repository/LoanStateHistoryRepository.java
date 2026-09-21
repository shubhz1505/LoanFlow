package com.loanflow.loan.repository;

import com.loanflow.loan.entity.LoanStateHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanStateHistoryRepository extends JpaRepository<LoanStateHistory, String> {

    List<LoanStateHistory> findByLoanApplicationIdOrderByTransitionedAtAsc(
            String loanApplicationId);
}