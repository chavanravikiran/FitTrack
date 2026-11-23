import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SKIP_ERROR_TOAST } from '../http/http-context';
import { ApiResponse } from '../models/api.models';
import { Goal, GoalRequest, GoalStatus, GoalUpdateRequest } from '../models/goal.models';

@Injectable({ providedIn: 'root' })
export class GoalService {
  private readonly api = `${environment.apiUrl}/me/goals`;

  constructor(private readonly http: HttpClient) {}

  list(status?: GoalStatus): Observable<Goal[]> {
    const params: Record<string, string> = status ? { status } : {};
    return this.http.get<ApiResponse<Goal[]>>(this.api, { params }).pipe(map((r) => r.data));
  }

  create(request: GoalRequest): Observable<Goal> {
    return this.http
      .post<ApiResponse<Goal>>(this.api, request, { context: new HttpContext().set(SKIP_ERROR_TOAST, true) })
      .pipe(map((r) => r.data));
  }

  update(id: number, request: GoalUpdateRequest): Observable<Goal> {
    return this.http
      .put<ApiResponse<Goal>>(`${this.api}/${id}`, request, { context: new HttpContext().set(SKIP_ERROR_TOAST, true) })
      .pipe(map((r) => r.data));
  }

  setStatus(id: number, status: GoalStatus): Observable<Goal> {
    return this.http.patch<ApiResponse<Goal>>(`${this.api}/${id}/status`, { status }).pipe(map((r) => r.data));
  }

  delete(id: number): Observable<void> {
    return this.http.delete<ApiResponse<void>>(`${this.api}/${id}`).pipe(map(() => undefined));
  }
}
