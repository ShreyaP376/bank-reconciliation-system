package com.bank.reconciliation.service;

import com.bank.reconciliation.entity.BankTransaction;
import com.opencsv.CSVReader;
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
public class CsvStatementParser {

    private static final Logger log = LoggerFactory.getLogger(CsvStatementParser.class);
    private static final DateTimeFormatter[] DATE_FORMATTERS = {
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy")
    };

    public List<BankTransaction> parse(InputStream inputStream) {
        List<BankTransaction> result = new ArrayList<>();
        try (var reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             CSVReader csv = new CSVReader(reader)) {
            String[] header = csv.readNext();
            if (header == null) return result;
            int idxId = indexOf(header, "transactionid", "id", "transaction_id");
            int idxDate = indexOf(header, "date");
            int idxTxDate = indexOf(header, "transaction_date", "transactiondate");
            int idxAmt = indexOf(header, "amount");
            int idxDesc = indexOf(header, "description", "desc");
            int idxRef = indexOf(header, "reference", "ref");

            // Prefer a generic "date" column, but also support "transaction_date"
            int effectiveDateIdx = idxDate >= 0 ? idxDate : idxTxDate;

            if (effectiveDateIdx < 0 || idxAmt < 0) {
                throw new IllegalArgumentException("Statement CSV must have columns for amount and a date (e.g. date or transaction_date)");
            }
            List<String[]> rows = csv.readAll();
            int line = 1;
            for (String[] row : rows) {
                line++;
                try {
                    String dateStr = get(row, effectiveDateIdx);
                    String amountStr = get(row, idxAmt);
                    if (dateStr == null || dateStr.isBlank() || amountStr == null || amountStr.isBlank()) {
                        log.warn("Statement line {} skipped: missing required field", line);
                        continue;
                    }
                    BigDecimal amount = new BigDecimal(amountStr.trim().replace(",", ""));
                    LocalDate date = parseDate(dateStr.trim());
                    BankTransaction tx = new BankTransaction();
                    tx.setExternalId(idxId >= 0 ? get(row, idxId) : null);
                    tx.setDate(date);
                    tx.setAmount(amount);
                    tx.setDescription(idxDesc >= 0 ? get(row, idxDesc) : null);
                    tx.setReference(idxRef >= 0 ? get(row, idxRef) : null);
                    result.add(tx);
                } catch (Exception e) {
                    log.warn("Statement line {} skipped: {}", line, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error parsing statement CSV", e);
            throw new RuntimeException("Failed to parse statement CSV: " + e.getMessage());
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
