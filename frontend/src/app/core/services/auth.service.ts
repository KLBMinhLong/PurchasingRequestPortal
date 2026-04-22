import { HttpClient } from '@angular/common/http';
import { Injectable, computed, signal } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, finalize, map, Observable, tap, throwError } from 'rxjs';

import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { AuthUser, LoginRequest, LoginResponse } from '../models/auth.model';
import { TokenStorageService } from './token-storage.service';

interface LogoutRequest {
  refreshToken?: string;
  idTokenHint?: string;
  postLogoutRedirectUri?: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly apiBaseUrl = environment.apiBaseUrl;

  private readonly userState = signal<AuthUser | null>(null);
  private readonly loggingOutState = signal(false);

  readonly user = computed(() => this.userState());
  readonly isAuthenticated = computed(() => !!this.tokenStorage.getToken() && !!this.userState());
  readonly isLoggingOut = computed(() => this.loggingOutState());

  constructor(
    private readonly httpClient: HttpClient,
    private readonly router: Router,
    private readonly tokenStorage: TokenStorageService
  ) {
    const roles = this.tokenStorage.getRoles();
    if (roles.length > 0) {
      this.userState.set({
        id: '',
        username: 'restored-session',
        email: '',
        roles
      });
    }
  }

  login(request: LoginRequest): Observable<LoginResponse> {
    return this.httpClient
      .post<ApiResponse<LoginResponse>>(`${this.apiBaseUrl}/api/auth/login`, request)
      .pipe(
        map((response) => response.data),
        tap((loginData) => {
          this.tokenStorage.saveToken(loginData.accessToken);
          this.tokenStorage.saveRoles(loginData.user.roles);
          this.userState.set(loginData.user);
        }),
        catchError((error) => {
          // Backend can reject login when same account is active on another device.
          // We keep message handling centralized here for login screens.
          if (error?.status === 409) {
            return throwError(() => new Error('Tai khoan dang duoc dang nhap tren thiet bi khac.'));
          }
          return throwError(() => error);
        })
      );
  }

  logout(payload?: LogoutRequest): Observable<void> {
    this.loggingOutState.set(true);
    return this.httpClient.post<ApiResponse<unknown>>(`${this.apiBaseUrl}/api/auth/logout`, payload ?? {}).pipe(
      tap(() => this.clearLocalSession()),
      map(() => void 0),
      catchError((error) => {
        this.clearLocalSession();
        return throwError(() => error);
      }),
      finalize(() => this.loggingOutState.set(false))
    );
  }

  forceLocalLogout(): void {
    this.clearLocalSession();
  }

  hasAnyRole(roles: string[]): boolean {
    if (roles.length === 0) {
      return true;
    }
    const currentRoles = this.tokenStorage.getRoles();
    return roles.some((role) => currentRoles.includes(role));
  }

  private clearLocalSession(): void {
    this.tokenStorage.clear();
    this.userState.set(null);
    void this.router.navigate(['/login']);
  }
}
