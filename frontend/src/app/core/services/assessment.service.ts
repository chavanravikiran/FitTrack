import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, map, shareReplay } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SKIP_ERROR_TOAST } from '../http/http-context';
import { ApiResponse, PageResponse } from '../models/api.models';
import { Assessment, AssessmentRequest, AssessmentSummary, Equipment } from '../models/assessment.models';

@Injectable({ providedIn: 'root' })
export class AssessmentService {
  private readonly api = environment.apiUrl;
  private equipment$?: Observable<Equipment[]>;

  constructor(private readonly http: HttpClient) {}

  /** Reference data - cached for the app lifetime. */
  equipment(): Observable<Equipment[]> {
    this.equipment$ ??= this.http
      .get<ApiResponse<Equipment[]>>(`${this.api}/equipment`)
      .pipe(map((r) => r.data), shareReplay(1));
    return this.equipment$;
  }

  current(): Observable<Assessment> {
    return this.http.get<ApiResponse<Assessment>>(`${this.api}/me/assessment`).pipe(map((r) => r.data));
  }

  reassess(request: AssessmentRequest): Observable<Assessment> {
    return this.http
      .post<ApiResponse<Assessment>>(`${this.api}/me/assessment`, request, {
        context: new HttpContext().set(SKIP_ERROR_TOAST, true),
      })
      .pipe(map((r) => r.data));
  }

  history(page = 0, size = 10): Observable<PageResponse<AssessmentSummary>> {
    return this.http
      .get<ApiResponse<PageResponse<AssessmentSummary>>>(`${this.api}/me/assessment/history`, {
        params: { page, size },
      })
      .pipe(map((r) => r.data));
  }
}
