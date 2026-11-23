import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SKIP_ERROR_TOAST } from '../http/http-context';
import { ApiResponse } from '../models/api.models';
import { Recommendations } from '../models/recommendation.models';

@Injectable({ providedIn: 'root' })
export class RecommendationService {
  constructor(private readonly http: HttpClient) {}

  get(): Observable<Recommendations> {
    return this.http
      .get<ApiResponse<Recommendations>>(`${environment.apiUrl}/me/recommendations`, {
        context: new HttpContext().set(SKIP_ERROR_TOAST, true),
      })
      .pipe(map((r) => r.data));
  }
}
