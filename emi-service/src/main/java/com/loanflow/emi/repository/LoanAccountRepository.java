package com.loanflow.emi.repository;

import com.loanflow.emi.entity.AccountStatus;
import com.loanflow.emi.entity.LoanAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoanAccountRepository
        extends JpaRepository<LoanAccount, String> {

    Optional<LoanAccount> findByLoanApplicationId(String loanApplicationId);

    List<LoanAccount> findByUserId(String userId);

    List<LoanAccount> findByAccountStatusIn(List<AccountStatus> statuses);
}