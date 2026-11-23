import { Component, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialog } from '@angular/material/dialog';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { Video } from '../../core/models/video.models';
import { ExerciseDetailDialogComponent } from '../../shared/components/exercise-detail-dialog.component';

/** Only these embed hosts are ever trusted for iframes. */
const TRUSTED_EMBEDS = [/^https:\/\/www\.youtube-nocookie\.com\/embed\/[A-Za-z0-9_-]{11}$/, /^https:\/\/player\.vimeo\.com\/video\/\d+$/];

@Component({
  selector: 'ft-video-player-dialog',
  standalone: false,
  template: `
    <h2 mat-dialog-title>{{ v.title }}</h2>
    <mat-dialog-content>
      @if (embedUrl) {
        <div class="frame">
          <iframe [src]="embedUrl" [title]="v.title" loading="lazy" referrerpolicy="strict-origin-when-cross-origin"
                  allow="accelerometer; encrypted-media; gyroscope; picture-in-picture; fullscreen" allowfullscreen></iframe>
        </div>
      } @else if (v.provider === 'SELF_HOSTED' && v.playUrl) {
        <video class="frame video" [src]="v.playUrl" controls preload="metadata" playsinline [attr.poster]="v.thumbnailUrl ?? null">
          Your browser can't play this video.
        </video>
      } @else {
        <div class="no-video"><mat-icon aria-hidden="true">menu_book</mat-icon> A video hasn't been added yet - follow the step-by-step guide below.</div>
      }
      <div class="chips">
        <span class="chip">{{ v.difficulty | titlecase }}</span>
        @if (v.muscleGroup) { <span class="chip">{{ v.muscleGroup.name }}</span> }
        @for (e of v.equipment; track e) { <span class="chip chip--outline">{{ e.toLowerCase().replace('_', ' ') }}</span> }
      </div>
      @if (v.instructions) {
        <h3>How to do it</h3>
        <p class="pre">{{ v.instructions }}</p>
      }
      @if (v.licenseNote) { <p class="ft-muted small">{{ v.licenseNote }}</p> }
      <p class="ft-muted small">Move within a pain-free range. Stop if you feel sharp pain, dizziness or chest discomfort.</p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      @if (v.exerciseId) {
        <button mat-button (click)="details()"><mat-icon>info</mat-icon> Mistakes & safety</button>
      }
      <button mat-flat-button mat-dialog-close>Close</button>
    </mat-dialog-actions>
  `,
  styles: `
    .frame { width: 100%; aspect-ratio: 16 / 9; border-radius: 16px; overflow: hidden; background: #000; display: block; }
    .frame iframe { width: 100%; height: 100%; border: 0; }
    .no-video { display: flex; gap: 10px; align-items: center; padding: 16px; border-radius: 16px; background: var(--mat-sys-surface-container); }
    .chips { display: flex; flex-wrap: wrap; gap: 6px; margin: 14px 0 4px; }
    .chip { padding: 2px 10px; border-radius: 999px; font: var(--mat-sys-label-medium); background: var(--mat-sys-secondary-container); color: var(--mat-sys-on-secondary-container); }
    .chip--outline { background: transparent; border: 1px solid var(--mat-sys-outline-variant); color: var(--mat-sys-on-surface); }
    h3 { font: var(--mat-sys-title-small); margin: 16px 0 4px; }
    .pre { white-space: pre-line; }
    .small { font: var(--mat-sys-body-small); }
  `,
})
export class VideoPlayerDialogComponent {
  readonly v = inject<Video>(MAT_DIALOG_DATA);
  readonly embedUrl: SafeResourceUrl | null;

  constructor(
    sanitizer: DomSanitizer,
    private readonly dialog: MatDialog,
  ) {
    const url = this.v.playUrl;
    const trusted = this.v.provider !== 'SELF_HOSTED' && !!url && TRUSTED_EMBEDS.some((re) => re.test(url));
    this.embedUrl = trusted ? sanitizer.bypassSecurityTrustResourceUrl(url!) : null;
  }

  details(): void {
    this.dialog.open(ExerciseDetailDialogComponent, { data: this.v.exerciseId, width: '640px', maxWidth: '95vw' });
  }
}
