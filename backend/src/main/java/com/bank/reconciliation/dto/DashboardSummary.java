package com.bank.reconciliation.dto;

import java.math.BigDecimal;

public class DashboardSummary {
    private long totalInvoices;
    private long totalTransactions;
    private long matchedInvoicesCount;
    private long matchedTransactionsCount;
    private BigDecimal totalInvoiceAmount;
    private BigDecimal totalTransactionAmount;
    private BigDecimal matchedAmount;
    private double matchPercentByCount;
    private double matchPercentByAmount;
    private BigDecimal outstandingBalance;
    private BigDecimal overpaymentCredits;

    public long getTotalInvoices() { return totalInvoices; }
    public void setTotalInvoices(long totalInvoices) { this.totalInvoices = totalInvoices; }
    public long getTotalTransactions() { return totalTransactions; }
    public void setTotalTransactions(long totalTransactions) { this.totalTransactions = totalTransactions; }
    public long getMatchedInvoicesCount() { return matchedInvoicesCount; }
    public void setMatchedInvoicesCount(long matchedInvoicesCount) { this.matchedInvoicesCount = matchedInvoicesCount; }
    public long getMatchedTransactionsCount() { return matchedTransactionsCount; }
    public void setMatchedTransactionsCount(long matchedTransactionsCount) { this.matchedTransactionsCount = matchedTransactionsCount; }
    public BigDecimal getTotalInvoiceAmount() { return totalInvoiceAmount; }
    public void setTotalInvoiceAmount(BigDecimal totalInvoiceAmount) { this.totalInvoiceAmount = totalInvoiceAmount; }
    public BigDecimal getTotalTransactionAmount() { return totalTransactionAmount; }
    public void setTotalTransactionAmount(BigDecimal totalTransactionAmount) { this.totalTransactionAmount = totalTransactionAmount; }
    public BigDecimal getMatchedAmount() { return matchedAmount; }
    public void setMatchedAmount(BigDecimal matchedAmount) { this.matchedAmount = matchedAmount; }
    public double getMatchPercentByCount() { return matchPercentByCount; }
    public void setMatchPercentByCount(double matchPercentByCount) { this.matchPercentByCount = matchPercentByCount; }
    public double getMatchPercentByAmount() { return matchPercentByAmount; }
    public void setMatchPercentByAmount(double matchPercentByAmount) { this.matchPercentByAmount = matchPercentByAmount; }
    public BigDecimal getOutstandingBalance() { return outstandingBalance; }
    public void setOutstandingBalance(BigDecimal outstandingBalance) { this.outstandingBalance = outstandingBalance; }
    public BigDecimal getOverpaymentCredits() { return overpaymentCredits; }
    public void setOverpaymentCredits(BigDecimal overpaymentCredits) { this.overpaymentCredits = overpaymentCredits; }
}
