package com.bank.reconciliation.service;

import com.bank.reconciliation.entity.*;
import com.bank.reconciliation.repository.*;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
public class ReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationService.class);
    private static final int FUZZY_CONFIDENCE_MIN = 75;
    private static final int FUZZY_CONFIDENCE_MAX = 90;
    private static final double FUZZY_SIMILARITY_THRESHOLD = 0.85;
    private static final int PARTIAL_CONFIDENCE = 85;
    private static final int DATE_TOLERANCE_DAYS = 2;

    private final InvoiceRepository invoiceRepository;
    private final BankTransactionRepository bankTransactionRepository;
    private final ReconciliationLinkRepository linkRepository;

    private final JaroWinklerSimilarity similarity = new JaroWinklerSimilarity();

    public ReconciliationService(InvoiceRepository invoiceRepository,
                                BankTransactionRepository bankTransactionRepository,
                                ReconciliationLinkRepository linkRepository) {
        this.invoiceRepository = invoiceRepository;
        this.bankTransactionRepository = bankTransactionRepository;
        this.linkRepository = linkRepository;
    }

    /**
     * Run full reconciliation: clear existing auto links, then apply rules in priority order.
     */
    @Transactional
    public void runReconciliation() {
        // Remove only auto-generated links (keep MANUAL_OVERRIDE)
        List<ReconciliationLink> all = linkRepository.findAll();
        for (ReconciliationLink link : all) {
            if (link.getMatchType() != MatchType.MANUAL_OVERRIDE) {
                Invoice inv = link.getInvoice();
                BankTransaction tx = link.getTransaction();
                inv.setMatchedAmount(inv.getMatchedAmount().subtract(link.getAmount()));
                tx.setMatchedAmount(tx.getMatchedAmount().subtract(link.getAmount()));
                invoiceRepository.save(inv);
                bankTransactionRepository.save(tx);
                linkRepository.delete(link);
            }
        }
        resetInvoiceAndTransactionStatusAndMatchedAmounts();

        List<Invoice> invoices = invoiceRepository.findAllByOrderById();
        List<BankTransaction> transactions = bankTransactionRepository.findAllByOrderById();

        Set<Long> matchedTransactionIds = new HashSet<>();
        Set<Long> matchedInvoiceIds = new HashSet<>();

        // 1) Exact match: same reference + same amount + same date
        for (Invoice inv : invoices) {
            if (matchedInvoiceIds.contains(inv.getId())) continue;
            for (BankTransaction tx : transactions) {
                if (matchedTransactionIds.contains(tx.getId())) continue;
                if (isExactMatch(inv, tx)) {
                    createLink(inv, tx, inv.getAmount(), MatchType.EXACT_MATCH, 100);
                    matchedTransactionIds.add(tx.getId());
                    matchedInvoiceIds.add(inv.getId());
                    break;
                }
            }
        }

        // 2) Fuzzy: description similarity > 85% + same amount + date ±2 days
        for (Invoice inv : invoices) {
            if (matchedInvoiceIds.contains(inv.getId())) continue;
            for (BankTransaction tx : transactions) {
                if (matchedTransactionIds.contains(tx.getId())) continue;
                int confidence = fuzzyMatchConfidence(inv, tx);
                if (confidence >= FUZZY_CONFIDENCE_MIN) {
                    createLink(inv, tx, inv.getAmount(), MatchType.FUZZY_MATCH, confidence);
                    matchedTransactionIds.add(tx.getId());
                    matchedInvoiceIds.add(inv.getId());
                    break;
                }
            }
        }

        // 3) Partial payment: multiple transactions (by absolute amount) sum to invoice amount
        for (Invoice inv : invoices) {
            if (matchedInvoiceIds.contains(inv.getId())) continue;
            List<BankTransaction> unmatched = transactions.stream()
                    .filter(t -> !matchedTransactionIds.contains(t.getId()))
                    .toList();
            List<BankTransaction> subset = findSubsetSum(unmatched, inv.getAmount(), inv.getDate());
            if (!subset.isEmpty()) {
                BigDecimal remaining = inv.getAmount();
                for (BankTransaction tx : subset) {
                    BigDecimal linkAmount = effectiveAmount(tx).min(remaining);
                    createLink(inv, tx, linkAmount, MatchType.PARTIAL_PAYMENT, PARTIAL_CONFIDENCE);
                    matchedTransactionIds.add(tx.getId());
                    remaining = remaining.subtract(linkAmount);
                    if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
                }
                matchedInvoiceIds.add(inv.getId());
            }
        }

        // 4) Overpayment: single transaction (by absolute amount) > invoice (link full invoice amount, rest is credit)
        for (Invoice inv : invoices) {
            if (matchedInvoiceIds.contains(inv.getId())) continue;
            for (BankTransaction tx : transactions) {
                if (matchedTransactionIds.contains(tx.getId())) continue;
                if (effectiveAmount(tx).compareTo(inv.getAmount()) >= 0 && datesWithinTolerance(inv.getDate(), tx.getDate())) {
                    createLink(inv, tx, inv.getAmount(), MatchType.OVERPAYMENT, 100);
                    matchedInvoiceIds.add(inv.getId());
                    // DO NOT add tx to matchedTransactionIds
                    break;

                }
            }
        }
        updateInvoiceAndTransactionStatuses();
    }

    private boolean isExactMatch(Invoice inv, BankTransaction tx) {
        return sameAmount(inv.getAmount(), effectiveAmount(tx))
                && inv.getDate().equals(tx.getDate())
                && sameReference(inv.getReference(), tx.getReference());
    }

    private boolean sameReference(String a, String b) {
        if (a == null) a = "";
        if (b == null) b = "";
        return a.trim().equalsIgnoreCase(b.trim());
    }

    /**
     * For reconciliation we treat outgoing payments (negative bank amounts)
     * as their absolute value when comparing to positive invoice amounts.
     */
    private BigDecimal effectiveAmount(BankTransaction tx) {
        return tx.getAmount() == null ? BigDecimal.ZERO : tx.getAmount().abs();
    }

    private boolean sameAmount(BigDecimal invoiceAmount, BigDecimal txEffectiveAmount) {
        return invoiceAmount.compareTo(txEffectiveAmount) == 0;
    }

    private boolean datesWithinTolerance(LocalDate d1, LocalDate d2) {
        long days = Math.abs(java.time.temporal.ChronoUnit.DAYS.between(d1, d2));
        return days <= DATE_TOLERANCE_DAYS;
    }

    private int fuzzyMatchConfidence(Invoice inv, BankTransaction tx) {
        if (!sameAmount(inv.getAmount(), effectiveAmount(tx)) || !datesWithinTolerance(inv.getDate(), tx.getDate())) {
            return 0;
        }
        String d1 = inv.getDescription() != null ? inv.getDescription() : inv.getReference();
        String d2 = tx.getDescription() != null ? tx.getDescription() : tx.getReference();
        if (d1 == null || d2 == null) return 0;
        double sim = similarity.apply(d1, d2);
        if (sim < FUZZY_SIMILARITY_THRESHOLD) return 0;
        return (int) (FUZZY_CONFIDENCE_MIN + (sim - FUZZY_SIMILARITY_THRESHOLD) / (1.0 - FUZZY_SIMILARITY_THRESHOLD) * (FUZZY_CONFIDENCE_MAX - FUZZY_CONFIDENCE_MIN));
    }

    private List<BankTransaction> findSubsetSum(List<BankTransaction> list, BigDecimal target, LocalDate invoiceDate) {
        BigDecimal tolerance = new BigDecimal("0.01");
        List<BankTransaction> best = new ArrayList<>();
        findSubsetSum(list, 0, BigDecimal.ZERO, target, invoiceDate, new ArrayList<>(), best, tolerance);
        return best;
    }

    private void findSubsetSum(List<BankTransaction> list, int idx, BigDecimal sum, BigDecimal target,
                               LocalDate invoiceDate, List<BankTransaction> current, List<BankTransaction> best,
                               BigDecimal tolerance) {
        if (sum.subtract(target).abs().compareTo(tolerance) <= 0 && !current.isEmpty()) {
            if (best.isEmpty() || current.size() < best.size()) {
                best.clear();
                best.addAll(current);
            }
            return;
        }
        if (idx >= list.size() || sum.compareTo(target) > 0) return;
        BankTransaction tx = list.get(idx);
        if (!datesWithinTolerance(invoiceDate, tx.getDate())) {
            findSubsetSum(list, idx + 1, sum, target, invoiceDate, current, best, tolerance);
            return;
        }
        current.add(tx);
        // Use absolute amount when building subset sums so negative "outgoing" payments can match positive invoices
        findSubsetSum(list, idx + 1, sum.add(effectiveAmount(tx)), target, invoiceDate, current, best, tolerance);
        current.remove(current.size() - 1);
        findSubsetSum(list, idx + 1, sum, target, invoiceDate, current, best, tolerance);
    }

    private void createLink(Invoice inv, BankTransaction tx, BigDecimal amount, MatchType type, int confidence) {
        ReconciliationLink link = new ReconciliationLink(inv, tx, amount, type, confidence);
        linkRepository.save(link);
        inv.getLinks().add(link);
        tx.getLinks().add(link);
        inv.setMatchedAmount(inv.getMatchedAmount().add(amount));
        tx.setMatchedAmount(tx.getMatchedAmount().add(amount));
        invoiceRepository.save(inv);
        bankTransactionRepository.save(tx);
    }

    private void resetInvoiceAndTransactionStatusAndMatchedAmounts() {
        for (Invoice inv : invoiceRepository.findAll()) {
            inv.setMatchedAmount(BigDecimal.ZERO);
            inv.setStatus(InvoiceStatus.UNPAID);
            inv.setConfidence(null);
            invoiceRepository.save(inv);
        }
        for (BankTransaction tx : bankTransactionRepository.findAll()) {
            tx.setMatchedAmount(BigDecimal.ZERO);
            tx.setStatus(TransactionStatus.UNMATCHED);
            bankTransactionRepository.save(tx);
        }
    }

//    private void updateInvoiceAndTransactionStatuses() {
//        for (Invoice inv : invoiceRepository.findAll()) {
//            inv.setStatus(deriveInvoiceStatus(inv));
//            invoiceRepository.save(inv);
//        }
//        for (BankTransaction tx : bankTransactionRepository.findAll()) {
//            tx.setStatus(deriveTransactionStatus(tx));
//            bankTransactionRepository.save(tx);
//        }
//    }
private void updateInvoiceAndTransactionStatuses() {

    // ---- INVOICES ----
    for (Invoice inv : invoiceRepository.findAll()) {
        BigDecimal matched = inv.getMatchedAmount();
        BigDecimal total = inv.getAmount();

        if (matched.compareTo(BigDecimal.ZERO) == 0) {
            inv.setStatus(InvoiceStatus.UNPAID);
        } else if (matched.compareTo(total) < 0) {
            inv.setStatus(InvoiceStatus.PARTIALLY_PAID);
        } else if (matched.compareTo(total) == 0) {
            inv.setStatus(InvoiceStatus.PAID);
        } else {
            inv.setStatus(InvoiceStatus.OVERPAID);
        }

        invoiceRepository.save(inv);
    }
    for (BankTransaction tx : bankTransactionRepository.findAll()) {
        BigDecimal matched = tx.getMatchedAmount();
        BigDecimal total = tx.getAmount().abs();

        if (tx.getAmount().signum() < 0) {
            tx.setStatus(TransactionStatus.OUTGOING);
        } else if (matched.compareTo(BigDecimal.ZERO) == 0) {
            tx.setStatus(TransactionStatus.UNMATCHED);
        } else if (matched.compareTo(total) < 0) {
            tx.setStatus(TransactionStatus.PARTIALLY_MATCHED);
        } else {
            tx.setStatus(TransactionStatus.MATCHED);
        }

        bankTransactionRepository.save(tx);
    }
}

    public List<Invoice> getInvoicesWithStatus() {
        return invoiceRepository.findAllByOrderById();
    }

    public List<BankTransaction> getTransactionsWithStatus() {
        return bankTransactionRepository.findAllByOrderById();
    }
}
