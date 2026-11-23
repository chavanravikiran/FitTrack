import { HttpErrorResponse, HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, switchMap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SKIP_AUTH_REFRESH } from '../http/http-context';
import { AuthService } from '../services/auth.service';

/**
 * Attaches the bearer token to API calls. On a 401 it refreshes the session once
 * (shared across concurrent requests) and retries; if refresh fails the user is signed out.
 */
@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(
    private readonly auth: AuthService,
    private readonly router: Router,
  ) {}

  intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    if (!req.url.startsWith(environment.apiUrl)) {
      return next.handle(req);
    }

    return next.handle(this.withToken(req, this.auth.token)).pipe(
      catchError((error: unknown) => {
        const unauthorized = error instanceof HttpErrorResponse && error.status === 401;
        if (!unauthorized || req.context.get(SKIP_AUTH_REFRESH)) {
          return throwError(() => error);
        }
        return this.auth.refresh().pipe(
          catchError((refreshError) => {
            this.auth.expireSession(this.router.url);
            return throwError(() => refreshError);
          }),
          switchMap((token) => next.handle(this.withToken(req, token))),
        );
      }),
    );
  }

  private withToken(req: HttpRequest<unknown>, token: string | null): HttpRequest<unknown> {
    return token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;
  }
}
