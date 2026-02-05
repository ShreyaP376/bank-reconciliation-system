package com.bank.reconciliation.repository;

import com.bank.reconciliation.entity.ReconciliationLink;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReconciliationLinkRepository extends JpaRepository<ReconciliationLink, Long> {
    List<ReconciliationLink> findByInvoiceId(Long invoiceId);
    List<ReconciliationLink> findByTransactionId(Long transactionId);
    void deleteByInvoiceIdAndTransactionId(Long invoiceId, Long transactionId);
}
