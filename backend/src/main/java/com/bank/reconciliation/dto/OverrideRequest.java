package com.bank.reconciliation.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class OverrideRequest {
    @NotNull
    private Long invoiceId;
    @NotNull
    private Long transactionId;
    private BigDecimal amount;
    private String reason;

    public Long getInvoiceId() { return invoiceId; }
    public void setInvoiceId(Long invoiceId) { this.invoiceId = invoiceId; }
    public Long getTransactionId() { return transactionId; }
    public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
