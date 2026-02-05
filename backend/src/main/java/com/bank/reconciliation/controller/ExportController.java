package com.bank.reconciliation.controller;

import com.bank.reconciliation.service.ExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/export")
public class ExportController {

    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    @GetMapping("/reconciliation-report")
    public ResponseEntity<byte[]> reconciliationReport() {
        byte[] csv = exportService.exportReconciliationReport();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reconciliation-report.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @GetMapping("/unmatched-invoices")
    public ResponseEntity<byte[]> unmatchedInvoices() {
        byte[] csv = exportService.exportUnmatchedInvoices();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=unmatched-invoices.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @GetMapping("/unmatched-transactions")
    public ResponseEntity<byte[]> unmatchedTransactions() {
        byte[] csv = exportService.exportUnmatchedTransactions();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=unmatched-transactions.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @GetMapping("/audit-log")
    public ResponseEntity<byte[]> auditLog() {
        byte[] csv = exportService.exportAuditLog();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=audit-log.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
