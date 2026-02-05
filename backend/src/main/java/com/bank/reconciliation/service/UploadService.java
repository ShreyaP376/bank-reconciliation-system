package com.bank.reconciliation.service;

import com.bank.reconciliation.entity.BankTransaction;
import com.bank.reconciliation.entity.Invoice;
import com.bank.reconciliation.repository.BankTransactionRepository;
import com.bank.reconciliation.repository.InvoiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class UploadService {

    private final CsvLedgerParser ledgerParser;
    private final CsvStatementParser statementParser;
    private final InvoiceRepository invoiceRepository;
    private final BankTransactionRepository bankTransactionRepository;

    public UploadService(CsvLedgerParser ledgerParser, CsvStatementParser statementParser,
                         InvoiceRepository invoiceRepository, BankTransactionRepository bankTransactionRepository) {
        this.ledgerParser = ledgerParser;
        this.statementParser = statementParser;
        this.invoiceRepository = invoiceRepository;
        this.bankTransactionRepository = bankTransactionRepository;
    }

    @Transactional
    public int uploadLedger(MultipartFile file) throws IOException {
        if (file.isEmpty()) throw new IllegalArgumentException("File is empty");
        List<Invoice> invoices = ledgerParser.parse(file.getInputStream());
        for (Invoice inv : invoices) {
            if (inv.getExternalId() != null && invoiceRepository.findByExternalId(inv.getExternalId()).isPresent()) {
                continue; // skip duplicate by external id
            }
            invoiceRepository.save(inv);
        }
        return invoices.size();
    }

    @Transactional
    public int uploadStatement(MultipartFile file) throws IOException {
        if (file.isEmpty()) throw new IllegalArgumentException("File is empty");
        List<BankTransaction> transactions = statementParser.parse(file.getInputStream());
        for (BankTransaction tx : transactions) {
            if (tx.getExternalId() != null && bankTransactionRepository.findByExternalId(tx.getExternalId()).isPresent()) {
                continue;
            }
            bankTransactionRepository.save(tx);
        }
        return transactions.size();
    }
}
