import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SKIP_ERROR_TOAST } from '../http/http-context';
import { ApiResponse, PageResponse } from '../models/api.models';
import {
  CalendarEntry,
  DayWorkout,
  ExerciseDetail,
  ExerciseSummary,
  HistoryItem,
  MuscleGroup,
  PersonalRecord,
  PlanExercise,
  SetLogRequest,
  WorkoutPlan,
  WorkoutSummary,
} from '../models/workout.models';

@Injectable({ providedIn: 'root' })
export class WorkoutService {
  private readonly api = environment.apiUrl;
  private readonly quiet = { context: new HttpContext().set(SKIP_ERROR_TOAST, true) };

  constructor(private readonly http: HttpClient) {}

  plan(): Observable<WorkoutPlan> {
    return this.http.get<ApiResponse<WorkoutPlan>>(`${this.api}/me/workout-plan`, this.quiet).pipe(map((r) => r.data));
  }

  regenerate(): Observable<WorkoutPlan> {
    return this.http.post<ApiResponse<WorkoutPlan>>(`${this.api}/me/workout-plan/regenerate`, null).pipe(map((r) => r.data));
  }

  swapExercise(planExerciseId: number, exerciseId: number): Observable<PlanExercise> {
    return this.http
      .put<ApiResponse<PlanExercise>>(`${this.api}/me/workout-plan/exercises/${planExerciseId}`, { exerciseId })
      .pipe(map((r) => r.data));
  }

  day(date?: string, planDayId?: number): Observable<DayWorkout> {
    const params: Record<string, string> = {};
    if (date) params['date'] = date;
    if (planDayId) params['planDayId'] = String(planDayId);
    return this.http.get<ApiResponse<DayWorkout>>(`${this.api}/me/workouts/day`, { params, ...this.quiet }).pipe(map((r) => r.data));
  }

  calendar(from: string, to: string): Observable<CalendarEntry[]> {
    return this.http
      .get<ApiResponse<CalendarEntry[]>>(`${this.api}/me/workouts/calendar`, { params: { from, to } })
      .pipe(map((r) => r.data));
  }

  summary(): Observable<WorkoutSummary> {
    return this.http.get<ApiResponse<WorkoutSummary>>(`${this.api}/me/workouts/summary`, this.quiet).pipe(map((r) => r.data));
  }

  start(date: string, planDayId?: number): Observable<DayWorkout> {
    return this.http.post<ApiResponse<DayWorkout>>(`${this.api}/me/workouts/start`, { date, planDayId }).pipe(map((r) => r.data));
  }

  logSets(sessionId: number, sets: SetLogRequest[]): Observable<DayWorkout> {
    return this.http
      .put<ApiResponse<DayWorkout>>(`${this.api}/me/workouts/sessions/${sessionId}/sets`, { sets })
      .pipe(map((r) => r.data));
  }

  complete(sessionId: number, perceivedEffort: number | null, notes: string | null): Observable<DayWorkout> {
    return this.http
      .post<ApiResponse<DayWorkout>>(`${this.api}/me/workouts/sessions/${sessionId}/complete`, { perceivedEffort, notes })
      .pipe(map((r) => r.data));
  }

  skip(date: string, planDayId: number, reason: string | null): Observable<CalendarEntry> {
    return this.http.post<ApiResponse<CalendarEntry>>(`${this.api}/me/workouts/skip`, { date, planDayId, reason }).pipe(map((r) => r.data));
  }

  reschedule(date: string, planDayId: number, newDate: string): Observable<CalendarEntry> {
    return this.http
      .post<ApiResponse<CalendarEntry>>(`${this.api}/me/workouts/reschedule`, { date, planDayId, newDate })
      .pipe(map((r) => r.data));
  }

  history(page = 0, size = 20): Observable<PageResponse<HistoryItem>> {
    return this.http
      .get<ApiResponse<PageResponse<HistoryItem>>>(`${this.api}/me/workouts/history`, { params: { page, size } })
      .pipe(map((r) => r.data));
  }

  personalRecords(): Observable<PersonalRecord[]> {
    return this.http.get<ApiResponse<PersonalRecord[]>>(`${this.api}/me/personal-records`).pipe(map((r) => r.data));
  }

  exercise(id: number): Observable<ExerciseDetail> {
    return this.http.get<ApiResponse<ExerciseDetail>>(`${this.api}/exercises/${id}`).pipe(map((r) => r.data));
  }

  exercises(filters: { muscle?: string; difficulty?: string; q?: string } = {}): Observable<ExerciseSummary[]> {
    const params: Record<string, string> = {};
    Object.entries(filters).forEach(([k, v]) => v && (params[k] = v));
    return this.http.get<ApiResponse<ExerciseSummary[]>>(`${this.api}/exercises`, { params }).pipe(map((r) => r.data));
  }

  muscleGroups(): Observable<MuscleGroup[]> {
    return this.http.get<ApiResponse<MuscleGroup[]>>(`${this.api}/muscle-groups`).pipe(map((r) => r.data));
  }
}
