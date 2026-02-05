package com.bank.reconciliation.repository;

import com.bank.reconciliation.entity.BankTransaction;
import com.bank.reconciliation.entity.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BankTransactionRepository extends JpaRepository<BankTransaction, Long> {
    Optional<BankTransaction> findByExternalId(String externalId);
    List<BankTransaction> findAllByOrderById();
    long countByStatus(TransactionStatus status);
}
