package com.bank.reconciliation.service;

import com.bank.reconciliation.entity.BankTransaction;
import com.bank.reconciliation.entity.Invoice;
import com.bank.reconciliation.entity.InvoiceStatus;
import com.bank.reconciliation.entity.TransactionStatus;
import com.bank.reconciliation.dto.DashboardSummary;
import com.bank.reconciliation.repository.BankTransactionRepository;
import com.bank.reconciliation.repository.InvoiceRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class DashboardService {

    private final InvoiceRepository invoiceRepository;
    private final BankTransactionRepository bankTransactionRepository;

    public DashboardService(InvoiceRepository invoiceRepository, BankTransactionRepository bankTransactionRepository) {
        this.invoiceRepository = invoiceRepository;
        this.bankTransactionRepository = bankTransactionRepository;
    }

    public DashboardSummary getSummary() {
        List<Invoice> invoices = invoiceRepository.findAllByOrderById();
        List<BankTransaction> transactions = bankTransactionRepository.findAllByOrderById();

        BigDecimal totalInvoiceAmount = invoices.stream().map(Invoice::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalTransactionAmount = transactions.stream().map(BankTransaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal matchedAmount = invoices.stream().map(Invoice::getMatchedAmount).filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);

        long matchedInvoices = invoices.stream().filter(i -> i.getMatchedAmount() != null && i.getMatchedAmount().compareTo(BigDecimal.ZERO) > 0).count();
        long matchedTransactions = transactions.stream().filter(t -> t.getMatchedAmount() != null && t.getMatchedAmount().compareTo(BigDecimal.ZERO) > 0).count();

        double matchPercentCount = 0;
        if (!invoices.isEmpty() || !transactions.isEmpty()) {
            long total = invoices.size() + transactions.size();
            long matched = matchedInvoices + matchedTransactions;
            matchPercentCount = total > 0 ? 100.0 * matched / (invoices.size() + transactions.size()) : 0;
        }
        double matchPercentAmount = 0;
        if (totalInvoiceAmount.compareTo(BigDecimal.ZERO) > 0) {
            matchPercentAmount = matchedAmount.divide(totalInvoiceAmount, 4, RoundingMode.HALF_UP).doubleValue() * 100;
        }

        BigDecimal outstanding = totalInvoiceAmount.subtract(matchedAmount);
        BigDecimal overpayment = invoices.stream()
                .filter(i -> i.getStatus() == InvoiceStatus.OVERPAID)
                .map(i -> i.getMatchedAmount().subtract(i.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        DashboardSummary s = new DashboardSummary();
        s.setTotalInvoices(invoices.size());
        s.setTotalTransactions(transactions.size());
        s.setMatchedInvoicesCount(matchedInvoices);
        s.setMatchedTransactionsCount(matchedTransactions);
        s.setTotalInvoiceAmount(totalInvoiceAmount);
        s.setTotalTransactionAmount(totalTransactionAmount);
        s.setMatchedAmount(matchedAmount);
        s.setMatchPercentByCount(matchPercentCount);
        s.setMatchPercentByAmount(matchPercentAmount);
        s.setOutstandingBalance(outstanding);
        s.setOverpaymentCredits(overpayment);
        return s;
    }
}
