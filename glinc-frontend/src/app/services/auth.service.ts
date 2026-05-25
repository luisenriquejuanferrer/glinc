import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { LoginResponse, MeResponse } from '../models/auth.model';

@Injectable({ providedIn: 'root' })
export class AuthService {

  private readonly apiBase = 'http://localhost:8080/api';
  private readonly tokenKey = 'glinc.authToken';
  private readonly emailKey = 'glinc.email';
  private readonly expiresAtKey = 'glinc.expiresAt';

  constructor(private http: HttpClient) {}

  login(email: string, password: string): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>(this.apiBase + '/auth/login', { email, password })
      .pipe(
        tap((res) => {
          localStorage.setItem(this.tokenKey, res.token);
          localStorage.setItem(this.emailKey, res.email);
          localStorage.setItem(this.expiresAtKey, res.expiresAt);
        })
      );
  }

  logout(): Observable<void> {
    const peticion = this.http.post<void>(this.apiBase + '/auth/logout', {});
    return peticion.pipe(
      tap({
        next: () => this.limpiarLocalStorage(),
        error: () => this.limpiarLocalStorage(),
      })
    );
  }

  me(): Observable<MeResponse> {
    return this.http.get<MeResponse>(this.apiBase + '/auth/me');
  }

  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  getEmail(): string | null {
    return localStorage.getItem(this.emailKey);
  }

  isLoggedIn(): boolean {
    const token = this.getToken();
    if (!token) {
      return false;
    }
    const expiresAt = localStorage.getItem(this.expiresAtKey);
    if (!expiresAt) {
      return true;
    }
    return new Date(expiresAt).getTime() > Date.now();
  }

  limpiarLocalStorage(): void {
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.emailKey);
    localStorage.removeItem(this.expiresAtKey);
  }
}
