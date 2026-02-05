import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';

const API = '/api';

@Component({
  selector: 'app-upload',
  standalone: true,
  imports: [CommonModule],
  template: `
    <h1>Upload CSV</h1>
    <div class="form-group">
      <label>Internal Ledger (Invoices)</label>
      <div class="upload-row">
        <input type="file" #ledgerInput accept=".csv" (change)="onLedgerFile(ledgerInput.files)" class="compact-file" />
        <button class="btn small" (click)="uploadLedger()" [disabled]="!ledgerFile || uploading">Upload</button>
        <span class="result" *ngIf="ledgerResult">{{ ledgerResult }}</span>
      </div>
    </div>
    <div class="form-group">
      <label>Bank Statement (Transactions)</label>
      <div class="upload-row">
        <input type="file" #stmtInput accept=".csv" (change)="onStatementFile(stmtInput.files)" class="compact-file" />
        <button class="btn small" (click)="uploadStatement()" [disabled]="!statementFile || uploading">Upload</button>
        <span class="result" *ngIf="statementResult">{{ statementResult }}</span>
      </div>
    </div>
    <p class="hint">CSV must have headers. Ledger: reference, amount, date (optional: invoiceId, description, customerName). Statement: date, amount (optional: transactionId, description, reference).</p>
  `,
})
export class UploadComponent {
  ledgerFile: File | null = null;
  statementFile: File | null = null;
  uploading = false;
  ledgerResult = '';
  statementResult = '';

  constructor(private http: HttpClient) {}

  onLedgerFile(files: FileList | null): void {
    this.ledgerFile = files?.length ? files[0] : null;
    this.ledgerResult = '';
  }

  onStatementFile(files: FileList | null): void {
    this.statementFile = files?.length ? files[0] : null;
    this.statementResult = '';
  }

  uploadLedger(): void {
    if (!this.ledgerFile) return;
    this.uploading = true;
    const form = new FormData();
    form.append('file', this.ledgerFile);
    this.http.post<{ uploaded: number }>(`${API}/upload/ledger`, form).subscribe({
      next: (r) => {
        this.ledgerResult = `Uploaded ${r.uploaded} rows.`;
        this.uploading = false;
      },
      error: (err) => {
        this.ledgerResult = err.error?.message || 'Upload failed';
        this.uploading = false;
      },
    });
  }

  uploadStatement(): void {
    if (!this.statementFile) return;
    this.uploading = true;
    const form = new FormData();
    form.append('file', this.statementFile);
    this.http.post<{ uploaded: number }>(`${API}/upload/statement`, form).subscribe({
      next: (r) => {
        this.statementResult = `Uploaded ${r.uploaded} rows.`;
        this.uploading = false;
      },
      error: (err) => {
        this.statementResult = err.error?.message || 'Upload failed';
        this.uploading = false;
      },
    });
  }
}
