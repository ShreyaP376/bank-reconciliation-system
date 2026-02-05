import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

const API = '/api';

export interface AuthResponse {
  token: string;
  email: string;
  role: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private token: string | null = null;
  private email: string | null = null;
  private role: string | null = null;

  constructor(private http: HttpClient) {
    this.token = localStorage.getItem('token');
    this.email = localStorage.getItem('email');
    this.role = localStorage.getItem('role');
  }

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${API}/auth/login`, { email, password }).pipe(
      tap((res) => this.setSession(res))
    );
  }

  register(email: string, password: string, role: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${API}/auth/register`, { email, password, role }).pipe(
      tap((res) => this.setSession(res))
    );
  }

  private setSession(res: AuthResponse): void {
    this.token = res.token;
    this.email = res.email;
    this.role = res.role;
    localStorage.setItem('token', res.token);
    localStorage.setItem('email', res.email);
    localStorage.setItem('role', res.role);
  }

  logout(): void {
    this.token = this.email = this.role = null;
    localStorage.removeItem('token');
    localStorage.removeItem('email');
    localStorage.removeItem('role');
  }

  getToken(): string | null {
    return this.token;
  }

  getEmail(): string | null {
    return this.email;
  }

  getRole(): string | null {
    return this.role;
  }

  canUpload(): boolean {
    return this.role === 'ADMIN' || this.role === 'EDITOR';
  }

  canReconcile(): boolean {
    return this.role === 'ADMIN' || this.role === 'EDITOR';
  }

  canOverride(): boolean {
    return this.role === 'ADMIN' || this.role === 'EDITOR';
  }
}
