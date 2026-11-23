import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api.models';
import { ActivityDay, ActivitySummary, Recovery, SleepEntry, WaterDay, WaterHistoryDay } from '../models/activity.models';

@Injectable({ providedIn: 'root' })
export class ActivityService {
  private readonly api = `${environment.apiUrl}/me`;

  constructor(private readonly http: HttpClient) {}

  // ---- steps ----
  saveSteps(date: string, steps: number, distanceM?: number | null, activeMinutes?: number | null): Observable<ActivityDay> {
    return this.http
      .put<ApiResponse<ActivityDay>>(`${this.api}/activity/${date}`, { steps, distanceM, activeMinutes })
      .pipe(map((r) => r.data));
  }

  stepSummary(days = 7): Observable<ActivitySummary> {
    return this.http.get<ApiResponse<ActivitySummary>>(`${this.api}/activity/summary`, { params: { days } }).pipe(map((r) => r.data));
  }

  // ---- water ----
  water(date?: string): Observable<WaterDay> {
    const params: Record<string, string> = date ? { date } : {};
    return this.http.get<ApiResponse<WaterDay>>(`${this.api}/water`, { params }).pipe(map((r) => r.data));
  }

  addWater(amountMl: number, date?: string): Observable<WaterDay> {
    return this.http.post<ApiResponse<WaterDay>>(`${this.api}/water`, { amountMl, date }).pipe(map((r) => r.data));
  }

  deleteWater(id: number): Observable<WaterDay> {
    return this.http.delete<ApiResponse<WaterDay>>(`${this.api}/water/${id}`).pipe(map((r) => r.data));
  }

  setWaterGoal(date: string, goalMl: number, reason: string | null): Observable<WaterDay> {
    return this.http.put<ApiResponse<WaterDay>>(`${this.api}/water/goal/${date}`, { goalMl, reason }).pipe(map((r) => r.data));
  }

  waterHistory(days = 7): Observable<WaterHistoryDay[]> {
    return this.http.get<ApiResponse<WaterHistoryDay[]>>(`${this.api}/water/history`, { params: { days } }).pipe(map((r) => r.data));
  }

  // ---- sleep ----
  saveSleep(date: string, body: { bedTime: string; wakeTime: string; quality: number | null; restDay: boolean; notes: string | null }): Observable<SleepEntry> {
    return this.http.put<ApiResponse<SleepEntry>>(`${this.api}/sleep/${date}`, body).pipe(map((r) => r.data));
  }

  sleepHistory(days = 14): Observable<SleepEntry[]> {
    return this.http.get<ApiResponse<SleepEntry[]>>(`${this.api}/sleep`, { params: { days } }).pipe(map((r) => r.data));
  }

  deleteSleep(date: string): Observable<void> {
    return this.http.delete<ApiResponse<void>>(`${this.api}/sleep/${date}`).pipe(map(() => undefined));
  }

  recovery(): Observable<Recovery> {
    return this.http.get<ApiResponse<Recovery>>(`${this.api}/recovery`).pipe(map((r) => r.data));
  }
}
