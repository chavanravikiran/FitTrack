import { HttpErrorResponse, HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, catchError, throwError } from 'rxjs';
import { SKIP_ERROR_TOAST } from '../http/http-context';
import { NotifyService } from '../services/notify.service';
import { apiErrorMessage } from '../utils/api-error';

/** Shows a friendly snackbar for failed requests unless the caller opted out. */
@Injectable()
export class ErrorInterceptor implements HttpInterceptor {
  constructor(private readonly notify: NotifyService) {}

  intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    return next.handle(req).pipe(
      catchError((error: unknown) => {
        if (error instanceof HttpErrorResponse && error.status === 402) {
          // Plan limit reached: always offer the upgrade path, even when the caller handles errors inline.
          this.notify.upgrade(apiErrorMessage(error));
        } else if (error instanceof HttpErrorResponse && !req.context.get(SKIP_ERROR_TOAST) && error.status !== 401) {
          this.notify.error(apiErrorMessage(error));
        }
        return throwError(() => error);
      }),
    );
  }
}
