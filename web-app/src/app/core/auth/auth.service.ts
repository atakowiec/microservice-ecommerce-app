import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { Observable, map } from 'rxjs';

import { API_ROUTES } from '../api/api.routes';
import {
  AuthSession,
  LoginCredentials,
  LoginResponse,
  UserRole,
} from './auth.models';

const SESSION_STORAGE_KEY = 'ecommerce.auth.session';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly sessionState = signal<AuthSession | null>(this.readStoredSession());

  readonly session = this.sessionState.asReadonly();
  readonly isAuthenticated = computed(() => this.sessionState() !== null);
  readonly username = computed(() => this.sessionState()?.username ?? null);

  login(credentials: LoginCredentials): Observable<AuthSession> {
    return this.http.post<LoginResponse>(API_ROUTES.auth.login, credentials).pipe(
      map((response) => {
        const session: AuthSession = {
          ...response,
          expiresAt: Date.now() + response.expiresIn * 1_000,
        };

        sessionStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(session));
        this.sessionState.set(session);
        return session;
      }),
    );
  }

  logout(): void {
    sessionStorage.removeItem(SESSION_STORAGE_KEY);
    this.sessionState.set(null);
  }

  hasRole(role: UserRole): boolean {
    return this.sessionState()?.role == role;
  }

  getAccessToken(): string | null {
    const session = this.sessionState();

    if (!session || this.isExpired(session)) {
      this.logout();
      return null;
    }

    return session.accessToken;
  }

  private readStoredSession(): AuthSession | null {
    const storedSession = sessionStorage.getItem(SESSION_STORAGE_KEY);

    if (!storedSession) {
      return null;
    }

    try {
      const session = JSON.parse(storedSession) as AuthSession;

      if (
        typeof session.accessToken !== 'string' ||
        typeof session.username !== 'string' ||
        this.isExpired(session)
      ) {
        sessionStorage.removeItem(SESSION_STORAGE_KEY);
        return null;
      }

      return session;
    } catch {
      sessionStorage.removeItem(SESSION_STORAGE_KEY);
      return null;
    }
  }

  private isExpired(session: AuthSession): boolean {
    return !Number.isFinite(session.expiresAt) || session.expiresAt <= Date.now();
  }
}
