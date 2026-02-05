package com.bank.reconciliation.repository;

import com.bank.reconciliation.entity.Invoice;
import com.bank.reconciliation.entity.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByExternalId(String externalId);
    List<Invoice> findAllByOrderById();
    long countByStatus(InvoiceStatus status);
}
