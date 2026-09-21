package com.loanflow.payment.repository;

import com.loanflow.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {

    List<Payment> findByLoanApplicationIdOrderByCreatedAtDesc(
            String loanApplicationId);

    boolean existsByTransactionReference(String transactionReference);
}