package com.bank.reconciliation.service;

import com.bank.reconciliation.entity.Invoice;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class CsvLedgerParser {

    private static final Logger log = LoggerFactory.getLogger(CsvLedgerParser.class);
    private static final DateTimeFormatter[] DATE_FORMATTERS = {
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy")
    };

    public List<Invoice> parse(InputStream inputStream) {
        List<Invoice> result = new ArrayList<>();
        try (var reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             CSVReader csv = new CSVReader(reader)) {
            String[] header = csv.readNext();
            if (header == null) return result;
            int idxId = indexOf(header, "invoiceid", "id", "invoice_id");
            int idxRef = indexOf(header, "reference", "ref");
            int idxAmt = indexOf(header, "amount");
            int idxDate = indexOf(header, "date");
            int idxInvoiceDate = indexOf(header, "invoice_date", "invoicedate");
            int idxDueDate = indexOf(header, "due_date", "duedate");
            int idxDesc = indexOf(header, "description", "desc");
            int idxCustomer = indexOf(header, "customername", "customer", "customer_name", "client_name", "clientname");

            // Prefer an explicit "date" column, but also support "invoice_date" or "due_date"
            int effectiveDateIdx = idxDate >= 0 ? idxDate : (idxInvoiceDate >= 0 ? idxInvoiceDate : idxDueDate);
            // If there is no dedicated reference column, fall back to the invoice id
            int effectiveRefIdx = idxRef >= 0 ? idxRef : idxId;

            if (idxAmt < 0 || effectiveDateIdx < 0) {
                throw new IllegalArgumentException("Ledger CSV must have columns for amount and a date (e.g. date, invoice_date or due_date)");
            }
            List<String[]> rows = csv.readAll();
            int line = 1;
            for (String[] row : rows) {
                line++;
                try {
                    String externalId = idxId >= 0 ? get(row, idxId) : null;
                    String ref = effectiveRefIdx >= 0 ? get(row, effectiveRefIdx) : null;
                    String amountStr = get(row, idxAmt);
                    String dateStr = get(row, effectiveDateIdx);
                    if (amountStr == null || amountStr.isBlank() || dateStr == null || dateStr.isBlank()) {
                        log.warn("Ledger line {} skipped: missing required field", line);
                        continue;
                    }
                    BigDecimal amount = new BigDecimal(amountStr.trim().replace(",", ""));
                    LocalDate date = parseDate(dateStr.trim());
                    Invoice inv = new Invoice();
                    inv.setExternalId(externalId);
                    // Use reference column if present, otherwise fall back to external id
                    String finalRef = (ref != null && !ref.isBlank()) ? ref.trim() : externalId;
                    inv.setReference(finalRef);
                    inv.setAmount(amount);
                    inv.setDate(date);
                    inv.setDescription(idxDesc >= 0 ? get(row, idxDesc) : null);
                    inv.setCustomerName(idxCustomer >= 0 ? get(row, idxCustomer) : null);
                    result.add(inv);
                } catch (Exception e) {
                    log.warn("Ledger line {} skipped: {}", line, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error parsing ledger CSV", e);
            throw new RuntimeException("Failed to parse ledger CSV: " + e.getMessage());
        }
        return result;
    }

    private static int indexOf(String[] header, String... names) {
        for (int i = 0; i < header.length; i++) {
            String h = header[i].trim().toLowerCase().replace("_", "").replace(" ", "");
            for (String n : names) {
                if (h.equals(n.replace("_", ""))) return i;
            }
        }
        return -1;
    }

    private static String get(String[] row, int idx) {
        if (idx < 0 || idx >= row.length) return null;
        String s = row[idx];
        return s == null ? null : s.trim();
    }

    private static LocalDate parseDate(String s) {
        for (DateTimeFormatter f : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(s, f);
            } catch (Exception ignored) {}
        }
        throw new IllegalArgumentException("Invalid date: " + s);
    }
}
