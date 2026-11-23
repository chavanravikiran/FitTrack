import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SKIP_ERROR_TOAST } from '../http/http-context';
import { AdminStats, AdminUserDetail, AdminUserRow, PlanUpdateRequest, UserStatusFilter } from '../models/admin.models';
import { ApiResponse, PageResponse } from '../models/api.models';
import { Payment, PaymentStatus, Plan } from '../models/billing.models';

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly api = `${environment.apiUrl}/admin`;

  constructor(private readonly http: HttpClient) {}

  stats(): Observable<AdminStats> {
    return this.http.get<ApiResponse<AdminStats>>(`${this.api}/stats`).pipe(map((r) => r.data));
  }

  users(q: string, plan: string, status: UserStatusFilter, page: number, size: number): Observable<PageResponse<AdminUserRow>> {
    return this.http
      .get<ApiResponse<PageResponse<AdminUserRow>>>(`${this.api}/users`, { params: { q, plan, status, page, size } })
      .pipe(map((r) => r.data));
  }

  user(id: number): Observable<AdminUserDetail> {
    return this.http.get<ApiResponse<AdminUserDetail>>(`${this.api}/users/${id}`).pipe(map((r) => r.data));
  }

  setEnabled(id: number, enabled: boolean): Observable<AdminUserRow> {
    return this.http.patch<ApiResponse<AdminUserRow>>(`${this.api}/users/${id}/status`, { enabled }).pipe(map((r) => r.data));
  }

  grant(id: number, planCode: string, days: number, note: string | null): Observable<AdminUserRow> {
    return this.http
      .post<ApiResponse<AdminUserRow>>(`${this.api}/users/${id}/grant`, { planCode, days, note }, {
        context: new HttpContext().set(SKIP_ERROR_TOAST, true),
      })
      .pipe(map((r) => r.data));
  }

  revoke(id: number): Observable<AdminUserRow> {
    return this.http.post<ApiResponse<AdminUserRow>>(`${this.api}/users/${id}/revoke`, null).pipe(map((r) => r.data));
  }

  plans(): Observable<Plan[]> {
    return this.http.get<ApiResponse<Plan[]>>(`${this.api}/plans`).pipe(map((r) => r.data));
  }

  updatePlan(code: string, req: PlanUpdateRequest): Observable<Plan> {
    return this.http
      .put<ApiResponse<Plan>>(`${this.api}/plans/${code}`, req, { context: new HttpContext().set(SKIP_ERROR_TOAST, true) })
      .pipe(map((r) => r.data));
  }

  payments(status: PaymentStatus | '', page: number, size: number): Observable<PageResponse<Payment>> {
    const params: Record<string, string | number> = { page, size };
    if (status) params['status'] = status;
    return this.http.get<ApiResponse<PageResponse<Payment>>>(`${this.api}/payments`, { params }).pipe(map((r) => r.data));
  }
}
