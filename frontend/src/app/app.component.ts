import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterOutlet, RouterLinkActive, Router } from '@angular/router';
import { AuthService } from './auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterOutlet, RouterLinkActive],
  template: `
    <header class="site-header" *ngIf="showHeader">
      <div class="brand">
        <a routerLink="/" class="brand-link">Automated Bank Reconciliation System</a>
      </div>

      <nav class="main-nav" *ngIf="auth.getToken()">
        <a routerLink="/dashboard" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: true }">Dashboard</a>
        <a routerLink="/upload" routerLinkActive="active" *ngIf="auth.canUpload()">Upload</a>
      </nav>

      <div class="auth-area">
        <a routerLink="/login" *ngIf="!auth.getToken()" class="login-link">Login</a>
        <ng-container *ngIf="auth.getToken()">
          <span class="user">{{ auth.getEmail() }} ({{ auth.getRole() }})</span>
          <button class="btn-logout" (click)="logout()">Logout</button>
        </ng-container>
      </div>
    </header>

    <main class="app-content">
      <router-outlet></router-outlet>
    </main>
  `,
  styles: [
    // kept empty here; styles are defined in global `styles.css` to maintain a consistent theme
  ],
})
export class AppComponent {
  constructor(public auth: AuthService, private router: Router) {}

  get showHeader(): boolean {
    return !this.router.url.startsWith('/login');
  }

  logout(): void {
    this.auth.logout();
    window.location.href = '/login';
  }
}
