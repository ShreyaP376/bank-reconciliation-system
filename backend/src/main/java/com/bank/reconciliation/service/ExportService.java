package com.bank.reconciliation.service;

import com.bank.reconciliation.entity.AuditLog;
import com.bank.reconciliation.entity.BankTransaction;
import com.bank.reconciliation.entity.Invoice;
import com.bank.reconciliation.repository.AuditLogRepository;
import com.bank.reconciliation.repository.BankTransactionRepository;
import com.bank.reconciliation.repository.InvoiceRepository;
import com.bank.reconciliation.repository.ReconciliationLinkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExportService {

    private final InvoiceRepository invoiceRepository;
    private final BankTransactionRepository bankTransactionRepository;
    private final ReconciliationLinkRepository linkRepository;
    private final AuditLogRepository auditLogRepository;

    public ExportService(InvoiceRepository invoiceRepository, BankTransactionRepository bankTransactionRepository,
                         ReconciliationLinkRepository linkRepository, AuditLogRepository auditLogRepository) {
        this.invoiceRepository = invoiceRepository;
        this.bankTransactionRepository = bankTransactionRepository;
        this.linkRepository = linkRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(readOnly = true)
    public byte[] exportReconciliationReport() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter w = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8));
        w.println("InvoiceId,Reference,Amount,Date,Status,MatchedAmount,TransactionIds,MatchType");
        List<Invoice> invoices = invoiceRepository.findAllByOrderById();
        for (Invoice inv : invoices) {
            String txIds = linkRepository.findByInvoiceId(inv.getId()).stream()
                    .map(l -> l.getTransaction().getId().toString())
                    .reduce((a, b) -> a + ";" + b).orElse("");
            String matchTypes = linkRepository.findByInvoiceId(inv.getId()).stream()
                    .map(l -> l.getMatchType().name())
                    .reduce((a, b) -> a + ";" + b).orElse("");
            w.printf("%s,%s,%s,%s,%s,%s,\"%s\",\"%s\"%n",
                    inv.getId(), escape(inv.getReference()), inv.getAmount(), inv.getDate(),
                    inv.getStatus(), inv.getMatchedAmount(), txIds, matchTypes);
        }
        w.flush();
        return baos.toByteArray();
    }

    public byte[] exportUnmatchedInvoices() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter w = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8));
        w.println("Id,Reference,Amount,Date,Description,CustomerName,Status");
        invoiceRepository.findAllByOrderById().stream()
                .filter(i -> i.getMatchedAmount() == null || i.getMatchedAmount().compareTo(java.math.BigDecimal.ZERO) == 0)
                .forEach(inv -> w.printf("%s,%s,%s,%s,%s,%s,%s%n",
                        inv.getId(), escape(inv.getReference()), inv.getAmount(), inv.getDate(),
                        escape(inv.getDescription()), escape(inv.getCustomerName()), inv.getStatus()));
        w.flush();
        return baos.toByteArray();
    }

    public byte[] exportUnmatchedTransactions() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter w = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8));
        w.println("Id,Date,Amount,Description,Reference,Status");
        bankTransactionRepository.findAllByOrderById().stream()
                .filter(t -> t.getMatchedAmount() == null || t.getMatchedAmount().compareTo(java.math.BigDecimal.ZERO) == 0)
                .forEach(tx -> w.printf("%s,%s,%s,%s,%s,%s%n",
                        tx.getId(), tx.getDate(), tx.getAmount(), escape(tx.getDescription()), escape(tx.getReference()), tx.getStatus()));
        w.flush();
        return baos.toByteArray();
    }

    @Transactional(readOnly = true)
    public byte[] exportAuditLog() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter w = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8));
        w.println("Id,UserId,Timestamp,Action,EntityType,EntityId,Reason,BeforeState,AfterState");
        DateTimeFormatter dtf = DateTimeFormatter.ISO_INSTANT;
        for (AuditLog log : auditLogRepository.findAllByOrderByTimestampDesc()) {
            String user = log.getUser() != null ? log.getUser().getEmail() : "";
            w.printf("%s,%s,%s,%s,%s,%s,%s,\"%s\",\"%s\"%n",
                    log.getId(), escape(user), log.getTimestamp() != null ? log.getTimestamp().toString() : "",
                    log.getAction(), log.getEntityType(), log.getEntityId(), escape(log.getReason()),
                    escape(log.getBeforeState()), escape(log.getAfterState()));
        }
        w.flush();
        return baos.toByteArray();
    }

    private static String escape(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
