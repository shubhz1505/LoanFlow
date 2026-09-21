package com.loanflow.document.repository;

import com.loanflow.commons.enums.DocumentType;
import com.loanflow.document.entity.LoanDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoanDocumentRepository
        extends JpaRepository<LoanDocument, String> {

    List<LoanDocument> findByLoanApplicationId(String loanApplicationId);

    Optional<LoanDocument> findByLoanApplicationIdAndDocumentType(
            String loanApplicationId, DocumentType documentType);

    boolean existsByLoanApplicationIdAndDocumentTypeAndVerifiedTrue(
            String loanApplicationId, DocumentType documentType);

    long countByLoanApplicationIdAndVerifiedTrue(String loanApplicationId);
}