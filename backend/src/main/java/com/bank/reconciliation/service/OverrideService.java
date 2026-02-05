package com.bank.reconciliation.service;

import com.bank.reconciliation.entity.*;
import com.bank.reconciliation.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OverrideService {

    private final InvoiceRepository invoiceRepository;
    private final BankTransactionRepository bankTransactionRepository;
    private final ReconciliationLinkRepository linkRepository;
    private final AuditService auditService;

    public OverrideService(InvoiceRepository invoiceRepository, BankTransactionRepository bankTransactionRepository,
                           ReconciliationLinkRepository linkRepository, AuditService auditService) {
        this.invoiceRepository = invoiceRepository;
        this.bankTransactionRepository = bankTransactionRepository;
        this.linkRepository = linkRepository;
        this.auditService = auditService;
    }

    @Transactional
    public void linkManually(String userEmail, Long invoiceId, Long transactionId, BigDecimal amount, String reason) {
        Invoice inv = invoiceRepository.findById(invoiceId).orElseThrow(() -> new IllegalArgumentException("Invoice not found"));
        BankTransaction tx = bankTransactionRepository.findById(transactionId).orElseThrow(() -> new IllegalArgumentException("Transaction not found"));
        String before = "invoiceId=" + invoiceId + ", transactionId=" + transactionId + ", linked=false";
        ReconciliationLink link = new ReconciliationLink(inv, tx, amount != null ? amount : tx.getAmount(), MatchType.MANUAL_OVERRIDE, 100);
        linkRepository.save(link);
        inv.setMatchedAmount(inv.getMatchedAmount().add(link.getAmount()));
        tx.setMatchedAmount(tx.getMatchedAmount().add(link.getAmount()));
        inv.setStatus(deriveInvoiceStatus(inv));
        tx.setStatus(deriveTransactionStatus(tx));
        invoiceRepository.save(inv);
        bankTransactionRepository.save(tx);
        String after = "linkId=" + link.getId() + ", amount=" + link.getAmount();
        auditService.log(userEmail, "MANUAL_LINK", "ReconciliationLink", link.getId(), reason, before, after);
    }

    @Transactional
    public void unlink(String userEmail, Long invoiceId, Long transactionId, String reason) {
        List<ReconciliationLink> links = linkRepository.findByInvoiceId(invoiceId).stream()
                .filter(l -> l.getTransaction().getId().equals(transactionId))
                .toList();
        if (links.isEmpty()) throw new IllegalArgumentException("No link found between invoice and transaction");
        ReconciliationLink link = links.get(0);
        String before = "linkId=" + link.getId() + ", amount=" + link.getAmount();
        Invoice inv = link.getInvoice();
        BankTransaction tx = link.getTransaction();
        inv.setMatchedAmount(inv.getMatchedAmount().subtract(link.getAmount()));
        tx.setMatchedAmount(tx.getMatchedAmount().subtract(link.getAmount()));
        inv.setStatus(deriveInvoiceStatus(inv));
        tx.setStatus(deriveTransactionStatus(tx));
        invoiceRepository.save(inv);
        bankTransactionRepository.save(tx);
        linkRepository.delete(link);
        auditService.log(userEmail, "MANUAL_UNLINK", "ReconciliationLink", link.getId(), reason, before, "deleted");
    }

    @Transactional
    public void addInvoiceNotes(String userEmail, Long invoiceId, String notes, String reason) {
        Invoice inv = invoiceRepository.findById(invoiceId).orElseThrow(() -> new IllegalArgumentException("Invoice not found"));
        String before = inv.getInternalNotes();
        inv.setInternalNotes(notes);
        invoiceRepository.save(inv);
        auditService.log(userEmail, "UPDATE_NOTES", "Invoice", invoiceId, reason, before, notes);
    }

    private InvoiceStatus deriveInvoiceStatus(Invoice inv) {
        if (inv.getMatchedAmount() == null || inv.getMatchedAmount().compareTo(BigDecimal.ZERO) == 0) return InvoiceStatus.UNPAID;
        int cmp = inv.getMatchedAmount().compareTo(inv.getAmount());
        if (cmp > 0) return InvoiceStatus.OVERPAID;
        if (cmp == 0) return InvoiceStatus.PAID;
        return InvoiceStatus.PARTIALLY_PAID;
    }

    private TransactionStatus deriveTransactionStatus(BankTransaction tx) {
        if (tx.getMatchedAmount() == null || tx.getMatchedAmount().compareTo(BigDecimal.ZERO) == 0)
            return tx.getAmount().compareTo(BigDecimal.ZERO) < 0 ? TransactionStatus.OUTGOING : TransactionStatus.UNMATCHED;
        if (tx.getMatchedAmount().compareTo(tx.getAmount()) >= 0) return TransactionStatus.MATCHED;
        return TransactionStatus.PARTIALLY_MATCHED;
    }
}
