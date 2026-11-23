import { HttpClient, HttpContext, HttpEvent, HttpHeaders, HttpRequest } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SKIP_ERROR_TOAST, SKIP_LOADING } from '../http/http-context';
import { ApiResponse } from '../models/api.models';
import { UploadTarget, Video, VideoCategory, VideoRequest } from '../models/video.models';

@Injectable({ providedIn: 'root' })
export class VideoService {
  private readonly api = environment.apiUrl;

  constructor(private readonly http: HttpClient) {}

  library(filters: { category?: VideoCategory | null; muscle?: string | null; difficulty?: string | null; q?: string | null } = {}): Observable<Video[]> {
    const params: Record<string, string> = {};
    Object.entries(filters).forEach(([k, v]) => v && (params[k] = String(v)));
    return this.http.get<ApiResponse<Video[]>>(`${this.api}/videos`, { params }).pipe(map((r) => r.data));
  }

  get(id: number): Observable<Video> {
    return this.http.get<ApiResponse<Video>>(`${this.api}/videos/${id}`).pipe(map((r) => r.data));
  }

  // ---- admin ----
  adminList(): Observable<Video[]> {
    return this.http.get<ApiResponse<Video[]>>(`${this.api}/admin/videos`).pipe(map((r) => r.data));
  }

  create(req: VideoRequest): Observable<Video> {
    return this.http
      .post<ApiResponse<Video>>(`${this.api}/admin/videos`, req, { context: new HttpContext().set(SKIP_ERROR_TOAST, true) })
      .pipe(map((r) => r.data));
  }

  update(id: number, req: VideoRequest): Observable<Video> {
    return this.http
      .put<ApiResponse<Video>>(`${this.api}/admin/videos/${id}`, req, { context: new HttpContext().set(SKIP_ERROR_TOAST, true) })
      .pipe(map((r) => r.data));
  }

  delete(id: number): Observable<void> {
    return this.http.delete<ApiResponse<void>>(`${this.api}/admin/videos/${id}`).pipe(map(() => undefined));
  }

  uploadThumbnail(id: number, file: File): Observable<Video> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<ApiResponse<Video>>(`${this.api}/admin/videos/${id}/thumbnail`, form).pipe(map((r) => r.data));
  }

  uploadTarget(id: number, contentType: string, sizeBytes: number): Observable<UploadTarget> {
    return this.http
      .post<ApiResponse<UploadTarget>>(`${this.api}/admin/videos/${id}/upload-url`, { contentType, sizeBytes }, {
        context: new HttpContext().set(SKIP_ERROR_TOAST, true),
      })
      .pipe(map((r) => r.data));
  }

  /** PUTs the file straight to S3 using the presigned URL (no auth header; reports progress). */
  uploadToStorage(target: UploadTarget, file: File): Observable<HttpEvent<unknown>> {
    const req = new HttpRequest(target.method, target.uploadUrl, file, {
      headers: new HttpHeaders({ 'Content-Type': target.contentType }),
      reportProgress: true,
      context: new HttpContext().set(SKIP_LOADING, true).set(SKIP_ERROR_TOAST, true),
    });
    return this.http.request(req);
  }

  confirmUpload(id: number, key: string): Observable<Video> {
    return this.http.post<ApiResponse<Video>>(`${this.api}/admin/videos/${id}/confirm-upload`, { key }).pipe(map((r) => r.data));
  }
}
