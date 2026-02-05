package com.bank.reconciliation.controller;

import com.bank.reconciliation.entity.BankTransaction;
import com.bank.reconciliation.entity.Invoice;
import com.bank.reconciliation.dto.DashboardSummary;
import com.bank.reconciliation.dto.InvoiceDto;
import com.bank.reconciliation.dto.TransactionDto;
import com.bank.reconciliation.service.DashboardService;
import com.bank.reconciliation.service.ReconciliationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final ReconciliationService reconciliationService;

    public DashboardController(DashboardService dashboardService, ReconciliationService reconciliationService) {
        this.dashboardService = dashboardService;
        this.reconciliationService = reconciliationService;
    }

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummary> summary() {
        return ResponseEntity.ok(dashboardService.getSummary());
    }

    @GetMapping("/invoices")
    public ResponseEntity<List<InvoiceDto>> invoices() {
        List<Invoice> list = reconciliationService.getInvoicesWithStatus();
        List<InvoiceDto> dtos = list.stream().map(this::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<TransactionDto>> transactions() {
        List<BankTransaction> list = reconciliationService.getTransactionsWithStatus();
        List<TransactionDto> dtos = list.stream().map(this::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/reconcile")
    public ResponseEntity<Void> runReconcile() {
        reconciliationService.runReconciliation();
        return ResponseEntity.ok().build();
    }

    private InvoiceDto toDto(Invoice inv) {
        InvoiceDto d = new InvoiceDto();
        d.setId(inv.getId());
        d.setExternalId(inv.getExternalId());
        d.setReference(inv.getReference());
        d.setAmount(inv.getAmount());
        d.setDate(inv.getDate());
        d.setDescription(inv.getDescription());
        d.setCustomerName(inv.getCustomerName());
        d.setStatus(inv.getStatus());
        d.setMatchedAmount(inv.getMatchedAmount());
        d.setConfidence(inv.getConfidence());
        d.setInternalNotes(inv.getInternalNotes());
        return d;
    }

    private TransactionDto toDto(BankTransaction tx) {
        TransactionDto d = new TransactionDto();
        d.setId(tx.getId());
        d.setExternalId(tx.getExternalId());
        d.setDate(tx.getDate());
        d.setAmount(tx.getAmount());
        d.setDescription(tx.getDescription());
        d.setReference(tx.getReference());
        d.setStatus(tx.getStatus());
        d.setMatchedAmount(tx.getMatchedAmount());
        return d;
    }
}
