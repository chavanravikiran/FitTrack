import { HttpEventType } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { filter, finalize, switchMap, tap } from 'rxjs';
import { Video, VideoCategory, VideoRequest } from '../../../core/models/video.models';
import { ExerciseSummary } from '../../../core/models/workout.models';
import { NotifyService } from '../../../core/services/notify.service';
import { VideoService } from '../../../core/services/video.service';
import { WorkoutService } from '../../../core/services/workout.service';
import { apiErrorMessage } from '../../../core/utils/api-error';
import { VIDEO_CATEGORIES } from '../../../shared/constants/labels';

@Component({
  selector: 'ft-video-edit-dialog',
  standalone: false,
  template: `
    <h2 mat-dialog-title>{{ video ? 'Edit video' : 'New video entry' }}</h2>
    <mat-dialog-content>
      @if (error) { <p class="error" role="alert">{{ error }}</p> }
      <form [formGroup]="form" id="video-form" class="ft-form" (ngSubmit)="save()">
        <mat-form-field appearance="outline"><mat-label>Title</mat-label><input matInput formControlName="title" maxlength="150" /></mat-form-field>
        <div class="ft-form-grid">
          <mat-form-field appearance="outline">
            <mat-label>Category</mat-label>
            <mat-select formControlName="category">@for (c of categories; track c.value) { <mat-option [value]="c.value">{{ c.label }}</mat-option> }</mat-select>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Difficulty</mat-label>
            <mat-select formControlName="difficulty">
              <mat-option value="BEGINNER">Beginner</mat-option><mat-option value="INTERMEDIATE">Intermediate</mat-option><mat-option value="ADVANCED">Advanced</mat-option>
            </mat-select>
          </mat-form-field>
        </div>
        <mat-form-field appearance="outline">
          <mat-label>Linked exercise (optional)</mat-label>
          <mat-select formControlName="exerciseId">
            <mat-option [value]="null">None</mat-option>
            @for (e of exercises; track e.id) { <mat-option [value]="e.id">{{ e.name }}</mat-option> }
          </mat-select>
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>YouTube or Vimeo link</mat-label>
          <input matInput formControlName="videoUrl" placeholder="https://www.youtube.com/watch?v=..." />
          <mat-hint>{{ video?.provider === 'SELF_HOSTED' ? 'Currently using an uploaded file - pasting a link replaces it.' : 'Embedded with YouTube privacy-enhanced mode.' }}</mat-hint>
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Instructions</mat-label>
          <textarea matInput formControlName="instructions" rows="5"></textarea>
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Licence / credit (optional)</mat-label>
          <input matInput formControlName="licenseNote" maxlength="255" placeholder="e.g. Video by Coach X, used with permission" />
        </mat-form-field>
        <mat-slide-toggle formControlName="active">Visible to users</mat-slide-toggle>
      </form>

      @if (video) {
        <h3>Files</h3>
        <div class="files">
          <div>
            <input #thumb type="file" accept="image/jpeg,image/png" hidden (change)="uploadThumbnail($event)" />
            <button mat-stroked-button type="button" (click)="thumb.click()" [disabled]="busy"><mat-icon>image</mat-icon> Upload thumbnail</button>
          </div>
          <div>
            <input #file type="file" accept="video/mp4,video/webm" hidden (change)="uploadVideo($event)" />
            <button mat-stroked-button type="button" (click)="file.click()" [disabled]="busy"><mat-icon>cloud_upload</mat-icon> Upload video to S3</button>
          </div>
        </div>
        @if (progress !== null) {
          <mat-progress-bar mode="determinate" [value]="progress" aria-label="Upload progress" />
          <p class="ft-muted small">Uploading directly to S3… {{ progress }}%</p>
        }
        <p class="ft-muted small">Videos (MP4/WebM) go straight from your browser to S3 using a short-lived signed URL. Requires STORAGE_TYPE=S3 on the server.</p>
      } @else {
        <p class="ft-muted small">Save the entry first, then you can upload a thumbnail or video file.</p>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button [mat-dialog-close]="changed">Close</button>
      <button mat-flat-button type="submit" form="video-form" [disabled]="form.invalid || busy">Save</button>
    </mat-dialog-actions>
  `,
  styles: `.error { color: var(--mat-sys-error); } h3 { font: var(--mat-sys-title-small); margin: 16px 0 8px; }
           .files { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 8px; } .small { font: var(--mat-sys-body-small); }`,
})
export class VideoEditDialogComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  video = inject<Video | null>(MAT_DIALOG_DATA);
  readonly categories = VIDEO_CATEGORIES;
  exercises: ExerciseSummary[] = [];
  busy = false;
  changed = false;
  progress: number | null = null;
  error: string | null = null;

  readonly form = this.fb.group({
    title: this.fb.nonNullable.control(this.video?.title ?? '', [Validators.required, Validators.maxLength(150)]),
    category: this.fb.nonNullable.control<VideoCategory>(this.video?.category ?? 'WARM_UP', Validators.required),
    difficulty: this.fb.nonNullable.control(this.video?.difficulty ?? 'BEGINNER', Validators.required),
    exerciseId: this.fb.control<number | null>(this.video?.exerciseId ?? null),
    videoUrl: this.fb.control<string | null>(this.video && this.video.provider !== 'SELF_HOSTED' ? this.video.playUrl ?? null : null),
    instructions: this.fb.control<string | null>(this.video?.instructions ?? null),
    licenseNote: this.fb.control<string | null>(this.video?.licenseNote ?? null),
    active: this.fb.nonNullable.control(this.video?.active ?? true),
  });

  constructor(
    private readonly videos: VideoService,
    private readonly workouts: WorkoutService,
    private readonly notify: NotifyService,
    private readonly ref: MatDialogRef<VideoEditDialogComponent, boolean>,
  ) {}

  ngOnInit(): void {
    this.workouts.exercises().subscribe((e) => (this.exercises = e));
  }

  save(): void {
    const v = this.form.getRawValue();
    const req: VideoRequest = {
      title: v.title.trim(), category: v.category, difficulty: v.difficulty, exerciseId: v.exerciseId,
      muscleGroupCode: null, videoUrl: v.videoUrl?.trim() || null, instructions: v.instructions?.trim() || null,
      licenseNote: v.licenseNote?.trim() || null, equipmentCodes: [], active: v.active,
    };
    this.busy = true;
    this.error = null;
    const call = this.video ? this.videos.update(this.video.id, req) : this.videos.create(req);
    call.pipe(finalize(() => (this.busy = false))).subscribe({
      next: (saved) => {
        this.video = saved;
        this.changed = true;
        this.notify.success('Saved');
      },
      error: (err) => (this.error = apiErrorMessage(err)),
    });
  }

  uploadThumbnail(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file || !this.video) return;
    this.busy = true;
    this.videos.uploadThumbnail(this.video.id, file).pipe(finalize(() => (this.busy = false))).subscribe((v) => {
      this.video = v;
      this.changed = true;
      this.notify.success('Thumbnail uploaded');
    });
  }

  uploadVideo(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file || !this.video) return;
    const id = this.video.id;
    this.busy = true;
    this.error = null;
    this.progress = 0;
    this.videos.uploadTarget(id, file.type, file.size).pipe(
      switchMap((target) => this.videos.uploadToStorage(target, file).pipe(
        tap((e) => {
          if (e.type === HttpEventType.UploadProgress && e.total) this.progress = Math.round((e.loaded / e.total) * 100);
        }),
        filter((e) => e.type === HttpEventType.Response),
        switchMap(() => this.videos.confirmUpload(id, target.key)),
      )),
      finalize(() => {
        this.busy = false;
        this.progress = null;
      }),
    ).subscribe({
      next: (v) => {
        this.video = v;
        this.changed = true;
        this.form.controls.videoUrl.setValue(null);
        this.notify.success('Video uploaded');
      },
      error: (err) => (this.error = apiErrorMessage(err, 'Upload failed. Check the S3 bucket CORS settings and try again.')),
    });
  }
}
