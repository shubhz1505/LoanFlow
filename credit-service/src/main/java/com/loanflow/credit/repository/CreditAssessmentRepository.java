package com.loanflow.credit.repository;

import com.loanflow.credit.entity.CreditAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CreditAssessmentRepository
        extends JpaRepository<CreditAssessment, String> {

    Optional<CreditAssessment> findByLoanApplicationId(String loanApplicationId);
}