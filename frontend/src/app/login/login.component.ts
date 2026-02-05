import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="login-box">
      <h1>Automated Bank Reconciliation System</h1>
      <p class="error" *ngIf="error">{{ error }}</p>
      <form (ngSubmit)="onSubmit()">
        <div class="form-group">
          <label>Email</label>
          <input type="email" [(ngModel)]="email" name="email" required />
        </div>
        <div class="form-group">
          <label>Password</label>
          <input type="password" [(ngModel)]="password" name="password" required />
        </div>
        <button type="submit" class="btn primary" [disabled]="loading">Login</button>
      </form>
      <p>Demo: admin&#64;test.com / admin | editor&#64;test.com / editor | viewer&#64;test.com / viewer</p>
    </div>
  `,
  styles: [
    '.login-box { max-width: 360px; margin: 2.5rem auto; padding: 1.5rem; background: var(--card); border: 1px solid var(--border); border-radius:10px; box-shadow: 0 8px 20px rgba(2,6,23,0.06); }',
    '.login-box h1 { margin: 0 0 0.5rem 0; font-size: 1.25rem; }',
    '.error { color: #ef4444; margin-bottom: 0.5rem; }',
  ],
})
export class LoginComponent {
  email = '';
  password = '';
  error = '';
  loading = false;

  constructor(private auth: AuthService, private router: Router) {}

  onSubmit(): void {
    this.error = '';
    this.loading = true;
    this.auth.login(this.email, this.password).subscribe({
      next: () => this.router.navigate(['/dashboard']),
      error: (err) => {
        this.error = err.error?.message || 'Login failed';
        this.loading = false;
      },
      complete: () => (this.loading = false),
    });
  }
}
