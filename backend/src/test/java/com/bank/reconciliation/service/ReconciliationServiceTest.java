package com.bank.reconciliation.service;

import com.bank.reconciliation.domain.*;
import com.bank.reconciliation.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReconciliationServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private BankTransactionRepository bankTransactionRepository;
    @Mock
    private ReconciliationLinkRepository linkRepository;

    private ReconciliationService service;

    @BeforeEach
    void setUp() {
        service = new ReconciliationService(invoiceRepository, bankTransactionRepository, linkRepository);
    }

    @Test
    void exactMatch_sameReferenceAmountDate_linksWith100Confidence() {
        Invoice inv = new Invoice();
        inv.setId(1L);
        inv.setReference("REF-001");
        inv.setAmount(new BigDecimal("100.00"));
        inv.setDate(LocalDate.of(2024, 1, 15));
        inv.setMatchedAmount(BigDecimal.ZERO);
        inv.setStatus(InvoiceStatus.UNPAID);

        BankTransaction tx = new BankTransaction();
        tx.setId(1L);
        tx.setReference("REF-001");
        tx.setAmount(new BigDecimal("100.00"));
        tx.setDate(LocalDate.of(2024, 1, 15));
        tx.setMatchedAmount(BigDecimal.ZERO);
        tx.setStatus(TransactionStatus.UNMATCHED);

        when(linkRepository.findAll()).thenReturn(List.of());
        when(invoiceRepository.findAll()).thenReturn(List.of(inv));
        when(bankTransactionRepository.findAll()).thenReturn(List.of(tx));
        when(invoiceRepository.findAllByOrderByDateDesc()).thenReturn(List.of(inv));
        when(bankTransactionRepository.findAllByOrderByDateDesc()).thenReturn(List.of(tx));
        when(invoiceRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(bankTransactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(linkRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.runReconciliation();

        verify(linkRepository, atLeastOnce()).save(argThat(link -> {
            ReconciliationLink l = (ReconciliationLink) link;
            return l.getMatchType() == MatchType.EXACT_MATCH
                    && l.getConfidence() == 100
                    && l.getAmount().compareTo(new BigDecimal("100.00")) == 0;
        }));
    }

    @Test
    void noMatch_differentReference_doesNotLink() {
        Invoice inv = new Invoice();
        inv.setId(1L);
        inv.setReference("INV-A");
        inv.setAmount(new BigDecimal("50.00"));
        inv.setDate(LocalDate.of(2024, 1, 10));
        inv.setMatchedAmount(BigDecimal.ZERO);

        BankTransaction tx = new BankTransaction();
        tx.setId(1L);
        tx.setReference("TX-B");
        tx.setAmount(new BigDecimal("50.00"));
        tx.setDate(LocalDate.of(2024, 1, 10));
        tx.setMatchedAmount(BigDecimal.ZERO);

        when(linkRepository.findAll()).thenReturn(List.of());
        when(invoiceRepository.findAll()).thenReturn(List.of(inv));
        when(bankTransactionRepository.findAll()).thenReturn(List.of(tx));
        when(invoiceRepository.findAllByOrderByDateDesc()).thenReturn(List.of(inv));
        when(bankTransactionRepository.findAllByOrderByDateDesc()).thenReturn(List.of(tx));
        when(invoiceRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(bankTransactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.runReconciliation();

        verify(linkRepository, never()).save(any());
    }

    @Test
    void partialPayment_multipleTransactionsSumToInvoice_linksWith85Confidence() {
        Invoice inv = new Invoice();
        inv.setId(1L);
        inv.setReference("INV-100");
        inv.setAmount(new BigDecimal("100.00"));
        inv.setDate(LocalDate.of(2024, 1, 15));
        inv.setMatchedAmount(BigDecimal.ZERO);

        BankTransaction t1 = new BankTransaction();
        t1.setId(1L);
        t1.setAmount(new BigDecimal("60.00"));
        t1.setDate(LocalDate.of(2024, 1, 16));
        t1.setMatchedAmount(BigDecimal.ZERO);

        BankTransaction t2 = new BankTransaction();
        t2.setId(2L);
        t2.setAmount(new BigDecimal("40.00"));
        t2.setDate(LocalDate.of(2024, 1, 14));
        t2.setMatchedAmount(BigDecimal.ZERO);

        when(linkRepository.findAll()).thenReturn(List.of());
        when(invoiceRepository.findAll()).thenReturn(List.of(inv));
        when(bankTransactionRepository.findAll()).thenReturn(List.of(t1, t2));
        when(invoiceRepository.findAllByOrderByDateDesc()).thenReturn(List.of(inv));
        when(bankTransactionRepository.findAllByOrderByDateDesc()).thenReturn(List.of(t1, t2));
        when(invoiceRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(bankTransactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(linkRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.runReconciliation();

        verify(linkRepository, atLeast(1)).save(argThat(link -> {
            ReconciliationLink l = (ReconciliationLink) link;
            return l.getMatchType() == MatchType.PARTIAL_PAYMENT && l.getConfidence() == 85;
        }));
    }
}
