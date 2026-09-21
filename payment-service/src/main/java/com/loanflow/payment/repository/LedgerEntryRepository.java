package com.loanflow.payment.repository;

import com.loanflow.payment.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface LedgerEntryRepository
        extends JpaRepository<LedgerEntry, String> {

    List<LedgerEntry> findByLoanApplicationIdOrderByCreatedAtAsc(
            String loanApplicationId);

    @Query("SELECT le.runningBalance FROM LedgerEntry le " +
            "WHERE le.loanApplicationId = :loanId " +
            "AND le.account = 'BORROWER_ACCOUNT' " +
            "ORDER BY le.createdAt DESC LIMIT 1")
    Optional<BigDecimal> getLatestBalanceForLoan(String loanId);

    @Query("SELECT le.entryType, SUM(le.amount) FROM LedgerEntry le " +
            "WHERE le.loanApplicationId = :loanId " +
            "GROUP BY le.entryType")
    List<Object[]> getLedgerSummary(String loanId);
}