import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, catchError, finalize, firstValueFrom, map, of, shareReplay, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SKIP_AUTH_REFRESH, SKIP_ERROR_TOAST, silentContext } from '../http/http-context';
import { ApiResponse } from '../models/api.models';
import {
  AuthResponse,
  ChangePasswordRequest,
  LoginRequest,
  RegisterRequest,
  ResetPasswordRequest,
  UserSummary,
} from '../models/auth.models';

/**
 * Owns the session. The access token lives only in memory (never localStorage) to limit XSS exposure;
 * the refresh token is an httpOnly cookie the browser sends to /api/v1/auth automatically.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly api = `${environment.apiUrl}/auth`;
  private accessToken: string | null = null;
  private refreshInFlight$: Observable<string> | null = null;
  private readonly userSubject = new BehaviorSubject<UserSummary | null>(null);

  readonly user$ = this.userSubject.asObservable();

  constructor(
    private readonly http: HttpClient,
    private readonly router: Router,
  ) {}

  get currentUser(): UserSummary | null {
    return this.userSubject.value;
  }

  get token(): string | null {
    return this.accessToken;
  }

  isAuthenticated(): boolean {
    return this.accessToken !== null && this.currentUser !== null;
  }

  isAdmin(): boolean {
    return this.currentUser?.roles.includes('ROLE_ADMIN') ?? false;
  }

  login(request: LoginRequest): Observable<UserSummary> {
    return this.http
      .post<ApiResponse<AuthResponse>>(`${this.api}/login`, request, {
        withCredentials: true,
        context: new HttpContext().set(SKIP_AUTH_REFRESH, true).set(SKIP_ERROR_TOAST, true),
      })
      .pipe(map((res) => this.startSession(res.data)));
  }

  register(request: RegisterRequest): Observable<UserSummary> {
    return this.http
      .post<ApiResponse<AuthResponse>>(`${this.api}/register`, request, {
        withCredentials: true,
        context: new HttpContext().set(SKIP_AUTH_REFRESH, true).set(SKIP_ERROR_TOAST, true),
      })
      .pipe(map((res) => this.startSession(res.data)));
  }

  /** Single-flight refresh: concurrent 401s share one refresh call. Emits the new access token. */
  refresh(): Observable<string> {
    if (!this.refreshInFlight$) {
      this.refreshInFlight$ = this.http
        .post<ApiResponse<AuthResponse>>(`${this.api}/refresh`, null, {
          withCredentials: true,
          context: silentContext(),
        })
        .pipe(
          map((res) => {
            this.startSession(res.data);
            return res.data.accessToken;
          }),
          finalize(() => (this.refreshInFlight$ = null)),
          shareReplay(1),
        );
    }
    return this.refreshInFlight$;
  }

  /** Called once at app start-up to resume a session from the refresh cookie. Never rejects. */
  restoreSession(): Promise<void> {
    return firstValueFrom(
      this.refresh().pipe(
        map(() => undefined),
        catchError(() => {
          this.clearSession();
          return of(undefined);
        }),
      ),
    );
  }

  logout(redirect = true): void {
    this.http
      .post(`${this.api}/logout`, null, { withCredentials: true, context: silentContext() })
      .pipe(catchError(() => of(null)))
      .subscribe(() => {
        this.clearSession();
        if (redirect) {
          this.router.navigate(['/auth/login']);
        }
      });
  }

  /** Local sign-out without calling the API (e.g. after refresh failed). */
  expireSession(returnUrl?: string): void {
    this.clearSession();
    this.router.navigate(['/auth/login'], {
      queryParams: returnUrl ? { returnUrl, expired: 1 } : { expired: 1 },
    });
  }

  forgotPassword(email: string): Observable<string> {
    return this.http
      .post<ApiResponse<void>>(`${this.api}/forgot-password`, { email }, {
        context: new HttpContext().set(SKIP_ERROR_TOAST, true),
      })
      .pipe(map((r) => r.message ?? ''));
  }

  resetPassword(request: ResetPasswordRequest): Observable<string> {
    return this.http
      .post<ApiResponse<void>>(`${this.api}/reset-password`, request, {
        context: new HttpContext().set(SKIP_ERROR_TOAST, true),
      })
      .pipe(map((r) => r.message ?? ''));
  }

  changePassword(request: ChangePasswordRequest): Observable<string> {
    return this.http
      .put<ApiResponse<void>>(`${this.api}/change-password`, request, {
        context: new HttpContext().set(SKIP_ERROR_TOAST, true),
      })
      .pipe(map((r) => r.message ?? ''));
  }

  verifyEmail(token: string): Observable<string> {
    return this.http
      .post<ApiResponse<void>>(`${this.api}/verify-email`, { token }, { context: silentContext() })
      .pipe(
        map((r) => r.message ?? ''),
        tap(() => this.patchUser({ emailVerified: true })),
      );
  }

  resendVerification(): Observable<string> {
    return this.http.post<ApiResponse<void>>(`${this.api}/resend-verification`, null).pipe(map((r) => r.message ?? ''));
  }

  /** Permanently deletes the account; the session ends locally once the server confirms. */
  deleteAccount(password: string): Observable<string> {
    return this.http
      .delete<ApiResponse<void>>(`${environment.apiUrl}/me/account`, {
        body: { password, confirmation: 'DELETE' },
        withCredentials: true,
        context: new HttpContext().set(SKIP_ERROR_TOAST, true),
      })
      .pipe(
        map((r) => r.message ?? ''),
        tap(() => this.clearSession()),
      );
  }

  /** Keeps the cached user in sync after onboarding/profile edits. */
  patchUser(patch: Partial<UserSummary>): void {
    const user = this.currentUser;
    if (user) {
      this.userSubject.next({ ...user, ...patch });
    }
  }

  private startSession(auth: AuthResponse): UserSummary {
    this.accessToken = auth.accessToken;
    this.userSubject.next(auth.user);
    return auth.user;
  }

  private clearSession(): void {
    this.accessToken = null;
    this.userSubject.next(null);
  }
}
