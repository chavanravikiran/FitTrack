import { Component, OnInit, inject } from '@angular/core';
import { MAT_DIALOG_DATA } from '@angular/material/dialog';
import { ExerciseDetail } from '../../core/models/workout.models';
import { WorkoutService } from '../../core/services/workout.service';
import { BODY_AREA_OPTIONS, labelOf } from '../constants/options';

/** Beginner-friendly exercise guide: how to, common mistakes, safety and alternatives. */
@Component({
  selector: 'ft-exercise-detail-dialog',
  standalone: false,
  template: `
    @if (detail; as d) {
      <h2 mat-dialog-title>{{ d.summary.name }}</h2>
      <mat-dialog-content>
        <div class="chips">
          <span class="chip">{{ d.summary.muscleGroup.name }}</span>
          <span class="chip">{{ d.summary.difficulty | titlecase }}</span>
          @for (e of equipment(d); track e) { <span class="chip chip--outline">{{ e }}</span> }
        </div>
        @if (d.description) { <p>{{ d.description }}</p> }
        @if (d.instructions) {
          <h3><mat-icon aria-hidden="true">format_list_numbered</mat-icon> How to do it</h3>
          <p class="pre">{{ d.instructions }}</p>
        }
        @if (d.commonMistakes) {
          <h3><mat-icon aria-hidden="true">error_outline</mat-icon> Common mistakes</h3>
          <p>{{ d.commonMistakes }}</p>
        }
        @if (d.safetyPrecautions) {
          <h3><mat-icon aria-hidden="true">health_and_safety</mat-icon> Safety</h3>
          <p>{{ d.safetyPrecautions }}</p>
        }
        @if (d.secondaryMuscles.length) {
          <p class="ft-muted small">Also works: {{ names(d) }}</p>
        }
        @if (d.avoidIf.length) {
          <p class="ft-muted small">Use caution or choose an alternative with: {{ avoid(d) }} issues.</p>
        }
        @if (d.alternatives.length) {
          <h3><mat-icon aria-hidden="true">swap_horiz</mat-icon> Alternatives</h3>
          <div class="chips">
            @for (a of d.alternatives; track a.id) { <span class="chip chip--outline">{{ a.name }}</span> }
          </div>
        }
        @if (d.videoId) {
          <a mat-stroked-button routerLink="/videos" [queryParams]="{ play: d.videoId }" mat-dialog-close class="video-link">
            <mat-icon>smart_display</mat-icon> Open in video library
          </a>
        }
      </mat-dialog-content>
      <mat-dialog-actions align="end"><button mat-button mat-dialog-close>Close</button></mat-dialog-actions>
    } @else {
      <mat-dialog-content><mat-progress-bar mode="indeterminate" /></mat-dialog-content>
    }
  `,
  styles: `
    .chips { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 8px; }
    .chip { padding: 2px 10px; border-radius: 999px; font: var(--mat-sys-label-medium); background: var(--mat-sys-secondary-container); color: var(--mat-sys-on-secondary-container); }
    .chip--outline { background: transparent; border: 1px solid var(--mat-sys-outline-variant); color: var(--mat-sys-on-surface); }
    h3 { display: flex; align-items: center; gap: 6px; font: var(--mat-sys-title-small); margin: 16px 0 4px; }
    h3 mat-icon { color: var(--mat-sys-primary); font-size: 20px; width: 20px; height: 20px; }
    .pre { white-space: pre-line; }
    .small { font: var(--mat-sys-body-small); }
    .video-link { margin-top: 12px; }
  `,
})
export class ExerciseDetailDialogComponent implements OnInit {
  private readonly id = inject<number>(MAT_DIALOG_DATA);
  detail: ExerciseDetail | null = null;

  constructor(private readonly workouts: WorkoutService) {}

  ngOnInit(): void {
    this.workouts.exercise(this.id).subscribe((d) => (this.detail = d));
  }

  equipment(d: ExerciseDetail): string[] {
    return d.summary.equipment.filter((e) => e !== 'NONE' && e !== 'YOGA_MAT').map((e) => e.toLowerCase().replace(/_/g, ' '));
  }

  names(d: ExerciseDetail): string {
    return d.secondaryMuscles.map((m) => m.name).join(', ');
  }

  avoid(d: ExerciseDetail): string {
    return d.avoidIf.map((a) => labelOf(BODY_AREA_OPTIONS, a as never).toLowerCase()).join(', ');
  }
}
