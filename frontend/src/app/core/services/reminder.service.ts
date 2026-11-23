import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, map, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SKIP_ERROR_TOAST, silentContext } from '../http/http-context';
import { ApiResponse, PageResponse } from '../models/api.models';
import { AppNotification, Reminder, ReminderRequest } from '../models/reminder.models';

@Injectable({ providedIn: 'root' })
export class ReminderService {
  private readonly api = `${environment.apiUrl}/me`;
  private readonly unreadSubject = new BehaviorSubject<number>(0);
  readonly unread$ = this.unreadSubject.asObservable();

  constructor(private readonly http: HttpClient) {}

  list(): Observable<Reminder[]> {
    return this.http.get<ApiResponse<Reminder[]>>(`${this.api}/reminders`).pipe(map((r) => r.data));
  }

  create(req: ReminderRequest): Observable<Reminder> {
    return this.http
      .post<ApiResponse<Reminder>>(`${this.api}/reminders`, req, { context: new HttpContext().set(SKIP_ERROR_TOAST, true) })
      .pipe(map((r) => r.data));
  }

  update(id: number, req: ReminderRequest): Observable<Reminder> {
    return this.http
      .put<ApiResponse<Reminder>>(`${this.api}/reminders/${id}`, req, { context: new HttpContext().set(SKIP_ERROR_TOAST, true) })
      .pipe(map((r) => r.data));
  }

  toggle(id: number, enabled: boolean): Observable<Reminder> {
    return this.http.patch<ApiResponse<Reminder>>(`${this.api}/reminders/${id}/toggle`, { enabled }).pipe(map((r) => r.data));
  }

  delete(id: number): Observable<void> {
    return this.http.delete<ApiResponse<void>>(`${this.api}/reminders/${id}`).pipe(map(() => undefined));
  }

  // ---- notifications ----
  notifications(unreadOnly = false, page = 0, size = 20): Observable<PageResponse<AppNotification>> {
    return this.http
      .get<ApiResponse<PageResponse<AppNotification>>>(`${this.api}/notifications`, { params: { unreadOnly, page, size } })
      .pipe(map((r) => r.data));
  }

  /** Background poll: silent (no spinner, no error toast, no refresh redirect loops). */
  pollUnread(): Observable<number> {
    return this.http
      .get<ApiResponse<{ unread: number }>>(`${this.api}/notifications/unread-count`, { context: silentContext() })
      .pipe(
        map((r) => r.data.unread),
        tap((n) => this.unreadSubject.next(n)),
      );
  }

  recentUnread(): Observable<AppNotification[]> {
    return this.http
      .get<ApiResponse<PageResponse<AppNotification>>>(`${this.api}/notifications`, {
        params: { unreadOnly: true, page: 0, size: 10 },
        context: silentContext(),
      })
      .pipe(map((r) => r.data.content));
  }

  markRead(id: number): Observable<AppNotification> {
    return this.http.patch<ApiResponse<AppNotification>>(`${this.api}/notifications/${id}/read`, null).pipe(
      map((r) => r.data),
      tap(() => this.unreadSubject.next(Math.max(0, this.unreadSubject.value - 1))),
    );
  }

  markAllRead(): Observable<void> {
    return this.http.post<ApiResponse<void>>(`${this.api}/notifications/read-all`, null).pipe(
      map(() => undefined),
      tap(() => this.unreadSubject.next(0)),
    );
  }
}
