import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../auth.service';

const API = '/api';

export interface DashboardSummary {
  totalInvoices: number;
  totalTransactions: number;
  matchedInvoicesCount: number;
  matchedTransactionsCount: number;
  totalInvoiceAmount: number;
  totalTransactionAmount: number;
  matchedAmount: number;
  matchPercentByCount: number;
  matchPercentByAmount: number;
  outstandingBalance: number;
  overpaymentCredits: number;
}

export interface InvoiceDto {
  id: number;
  reference: string;
  amount: number;
  date: string;
  status: string;
  matchedAmount: number | null;
  confidence: number | null;
  internalNotes: string | null;
}

export interface TransactionDto {
  id: number;
  date: string;
  amount: number;
  description: string | null;
  reference: string | null;
  status: string;
  matchedAmount: number | null;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './dashboard.component.html',
})
export class DashboardComponent implements OnInit {
  summary: DashboardSummary | null = null;
  invoices: InvoiceDto[] = [];
  transactions: TransactionDto[] = [];
  loading = false;
  filterInvoiceStatus = '';
  filterTransactionStatus = '';
  overrideInvoiceId: number | null = null;
  overrideTransactionId: number | null = null;
  overrideAmount = '';
  overrideReason = '';
  showOverrideDialog = false;
  overrideError = '';

  constructor(private http: HttpClient, public auth: AuthService) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.http.get<DashboardSummary>(`${API}/dashboard/summary`).subscribe({
      next: (s) => {
        this.summary = s;
        this.loading = false;
      },
      error: () => (this.loading = false),
    });
    this.http.get<InvoiceDto[]>(`${API}/dashboard/invoices`).subscribe((list) => (this.invoices = list));
    this.http.get<TransactionDto[]>(`${API}/dashboard/transactions`).subscribe((list) => (this.transactions = list));
  }

  runReconcile(): void {
    if (!this.auth.canReconcile()) return;
    this.loading = true;
    this.http.post(`${API}/dashboard/reconcile`, {}).subscribe({
      next: () => this.load(),
      error: () => (this.loading = false),
    });
  }

  openOverride(inv: InvoiceDto, tx: TransactionDto): void {
    this.overrideInvoiceId = inv.id;
    this.overrideTransactionId = tx.id;
    this.overrideAmount = String(tx.amount);
    this.overrideReason = '';
    this.overrideError = '';
    this.showOverrideDialog = true;
  }

  closeOverride(): void {
    this.showOverrideDialog = false;
  }

  submitLink(): void {
    if (this.overrideInvoiceId == null || this.overrideTransactionId == null) {
      this.overrideError = 'Select a transaction';
      return;
    }
    this.overrideError = '';
    const body = {
      invoiceId: this.overrideInvoiceId,
      transactionId: this.overrideTransactionId,
      amount: this.overrideAmount ? parseFloat(this.overrideAmount) : null,
      reason: this.overrideReason || 'Manual link',
    };
    this.http.post(`${API}/override/link`, body).subscribe({
      next: () => {
        this.closeOverride();
        this.load();
      },
      error: (err) => (this.overrideError = err.error?.message || 'Failed'),
    });
  }

  exportReport(): void {
    this.http.get(API + '/export/reconciliation-report', { responseType: 'blob' }).subscribe((blob) => {
      const a = document.createElement('a');
      a.href = URL.createObjectURL(blob);
      a.download = 'reconciliation-report.csv';
      a.click();
      URL.revokeObjectURL(a.href);
    });
  }

  exportUnmatchedInvoices(): void {
    this.http.get(API + '/export/unmatched-invoices', { responseType: 'blob' }).subscribe((blob) => {
      const a = document.createElement('a');
      a.href = URL.createObjectURL(blob);
      a.download = 'unmatched-invoices.csv';
      a.click();
      URL.revokeObjectURL(a.href);
    });
  }

  exportUnmatchedTransactions(): void {
    this.http.get(API + '/export/unmatched-transactions', { responseType: 'blob' }).subscribe((blob) => {
      const a = document.createElement('a');
      a.href = URL.createObjectURL(blob);
      a.download = 'unmatched-transactions.csv';
      a.click();
      URL.revokeObjectURL(a.href);
    });
  }

  exportAudit(): void {
    this.http.get(API + '/export/audit-log', { responseType: 'blob' }).subscribe((blob) => {
      const a = document.createElement('a');
      a.href = URL.createObjectURL(blob);
      a.download = 'audit-log.csv';
      a.click();
      URL.revokeObjectURL(a.href);
    });
  }

  get filteredInvoices(): InvoiceDto[] {
    if (!this.filterInvoiceStatus) return this.invoices;
    return this.invoices.filter((i) => i.status === this.filterInvoiceStatus);
  }

  get filteredTransactions(): TransactionDto[] {
    if (!this.filterTransactionStatus) return this.transactions;
    return this.transactions.filter((t) => t.status === this.filterTransactionStatus);
  }

  onTxChange(inv: any, txId: string) {
  const tx = this.transactions.find(t => t.id === Number(txId));
  if (tx) {
    this.openOverride(inv, tx); 
  }
}

}
