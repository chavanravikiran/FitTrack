import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, map, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SKIP_ERROR_TOAST } from '../http/http-context';
import { ApiResponse } from '../models/api.models';
import { OnboardingRequest } from '../models/assessment.models';
import { Profile, ThemePreference, UpdateProfileRequest } from '../models/profile.models';

@Injectable({ providedIn: 'root' })
export class ProfileService {
  private readonly api = `${environment.apiUrl}/me`;
  private readonly profileSubject = new BehaviorSubject<Profile | null>(null);

  /** Last loaded profile, shared by the shell (avatar/name) and pages. */
  readonly profile$ = this.profileSubject.asObservable();

  constructor(private readonly http: HttpClient) {}

  load(): Observable<Profile> {
    return this.http.get<ApiResponse<Profile>>(`${this.api}/profile`).pipe(
      map((r) => r.data),
      tap((p) => this.profileSubject.next(p)),
    );
  }

  update(request: UpdateProfileRequest): Observable<Profile> {
    return this.http
      .put<ApiResponse<Profile>>(`${this.api}/profile`, request, {
        context: new HttpContext().set(SKIP_ERROR_TOAST, true),
      })
      .pipe(
        map((r) => r.data),
        tap((p) => this.profileSubject.next(p)),
      );
  }

  updateTheme(theme: ThemePreference): Observable<Profile> {
    return this.http.patch<ApiResponse<Profile>>(`${this.api}/profile/theme`, { theme }).pipe(
      map((r) => r.data),
      tap((p) => this.profileSubject.next(p)),
    );
  }

  completeOnboarding(request: OnboardingRequest): Observable<Profile> {
    return this.http
      .post<ApiResponse<Profile>>(`${this.api}/onboarding`, request, {
        context: new HttpContext().set(SKIP_ERROR_TOAST, true),
      })
      .pipe(
        map((r) => r.data),
        tap((p) => this.profileSubject.next(p)),
      );
  }

  /** Bumped on every upload so avatar consumers know to re-fetch the image. */
  photoVersion = 0;

  uploadPhoto(file: File): Observable<Profile> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<ApiResponse<Profile>>(`${this.api}/profile/photo`, form).pipe(
      map((r) => r.data),
      tap((p) => {
        this.photoVersion++;
        this.profileSubject.next(p);
      }),
    );
  }

  /** The photo endpoint requires the bearer token, so it's fetched as a blob rather than used in <img src>. */
  photoBlob(): Observable<Blob> {
    return this.http.get(`${this.api}/profile/photo`, { responseType: 'blob' });
  }

  deletePhoto(): Observable<void> {
    return this.http.delete<ApiResponse<void>>(`${this.api}/profile/photo`).pipe(
      map(() => undefined),
      tap(() => {
        const p = this.profileSubject.value;
        if (p) this.profileSubject.next({ ...p, hasPhoto: false });
      }),
    );
  }

  clear(): void {
    this.profileSubject.next(null);
  }
}
