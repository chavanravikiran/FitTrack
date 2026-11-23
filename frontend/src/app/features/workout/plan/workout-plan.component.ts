import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { finalize } from 'rxjs';
import { ExerciseSummary, PlanExercise, WorkoutPlan } from '../../../core/models/workout.models';
import { BillingService } from '../../../core/services/billing.service';
import { NotifyService } from '../../../core/services/notify.service';
import { WorkoutService } from '../../../core/services/workout.service';
import { apiErrorMessage } from '../../../core/utils/api-error';
import { ExerciseDetailDialogComponent } from '../../../shared/components/exercise-detail-dialog.component';
import { AlternativesDialogComponent, AlternativesDialogData } from '../dialogs/alternatives-dialog.component';

const DAY_NAMES = ['', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];

@Component({
  selector: 'ft-workout-plan',
  standalone: false,
  template: `
    @if (error) {
      <mat-card appearance="outlined" class="ft-card">
        <ft-empty-state icon="assignment" title="No plan yet" [message]="error">
          <a mat-flat-button routerLink="/profile" [queryParams]="{ tab: 'assessment' }">Complete my assessment</a>
        </ft-empty-state>
      </mat-card>
    } @else if (plan) {
      <mat-card appearance="outlined" class="ft-card why">
        <div class="why__head">
          <div>
            <h2 class="ft-section-title">{{ plan.name }}</h2>
            <p class="ft-muted small">Built {{ plan.startDate | date: 'mediumDate' }} from your fitness assessment.</p>
          </div>
          <button mat-stroked-button (click)="regenerate()" [disabled]="busy"><mat-icon>refresh</mat-icon> Rebuild plan</button>
        </div>
        <ul class="reasons">
          @for (line of plan.explanations; track line) { <li><mat-icon aria-hidden="true">check</mat-icon>{{ line }}</li> }
        </ul>
        @if (!canCustomise) {
          <p class="ft-muted small"><mat-icon inline>workspace_premium</mat-icon> Premium lets you swap exercises in your plan permanently.
            You can always use alternatives during a session.</p>
        }
      </mat-card>

      <div class="days">
        @for (d of plan.days; track d.id) {
          <mat-card appearance="outlined" class="day" [class.day--rest]="d.restDay">
            <div class="day__head">
              <span class="dow">{{ dayName(d.dayOfWeek) }}</span>
              <strong>{{ d.title }}</strong>
            </div>
            @if (d.restDay) {
              <p class="ft-muted small">{{ d.focus }}</p>
            } @else {
              <ol class="list">
                @for (e of d.exercises; track e.id) {
                  <li>
                    <button class="link" type="button" (click)="info(e.exercise)">{{ e.exercise.name }}</button>
                    <span class="ft-muted small">{{ target(e) }}</span>
                    @if (canCustomise) {
                      <button mat-icon-button class="swap" (click)="swap(e)" [attr.aria-label]="'Swap ' + e.exercise.name" matTooltip="Swap exercise"><mat-icon>swap_horiz</mat-icon></button>
                    }
                  </li>
                }
              </ol>
            }
          </mat-card>
        }
      </div>
    } @else {
      <mat-progress-bar mode="indeterminate" />
    }
  `,
  styles: `
    .why { margin-bottom: 16px; }
    .why__head { display: flex; justify-content: space-between; gap: 12px; flex-wrap: wrap; align-items: flex-start; }
    .why__head .ft-section-title { margin: 0; }
    .reasons { list-style: none; padding: 0; margin: 12px 0 0; display: flex; flex-direction: column; gap: 8px; }
    .reasons li { display: flex; gap: 8px; align-items: flex-start; }
    .reasons mat-icon { color: var(--mat-sys-primary); font-size: 20px; width: 20px; height: 20px; flex: none; }
    .small { font: var(--mat-sys-body-small); }
    .days { display: grid; gap: 12px; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); }
    .day { padding: 16px; border-radius: 20px; }
    .day--rest { opacity: .75; }
    .day__head { display: flex; flex-direction: column; margin-bottom: 8px; }
    .dow { font: var(--mat-sys-label-medium); color: var(--mat-sys-primary); text-transform: uppercase; letter-spacing: .04em; }
    .list { margin: 0; padding-left: 20px; display: flex; flex-direction: column; gap: 6px; }
    .list li { position: relative; padding-right: 36px; }
    .link { all: unset; cursor: pointer; font-weight: 600; display: block; }
    .link:hover, .link:focus-visible { color: var(--mat-sys-primary); text-decoration: underline; }
    .swap { position: absolute; right: -8px; top: -6px; }
  `,
})
export class WorkoutPlanComponent implements OnInit {
  plan: WorkoutPlan | null = null;
  error: string | null = null;
  busy = false;

  constructor(
    private readonly workouts: WorkoutService,
    private readonly billing: BillingService,
    private readonly dialog: MatDialog,
    private readonly notify: NotifyService,
  ) {}

  get canCustomise(): boolean {
    return this.billing.hasFeature('CUSTOM_WORKOUTS');
  }

  ngOnInit(): void {
    this.workouts.plan().subscribe({
      next: (p) => (this.plan = p),
      error: (err) => (this.error = apiErrorMessage(err)),
    });
  }

  dayName(d: number): string {
    return DAY_NAMES[d];
  }

  target(e: PlanExercise): string {
    if (e.durationSec) return `${e.sets} × ${e.durationSec >= 60 ? Math.round(e.durationSec / 60) + ' min' : e.durationSec + 's'}`;
    return `${e.sets} × ${e.repsMin}-${e.repsMax} reps · rest ${e.restSec}s`;
  }

  info(ex: ExerciseSummary): void {
    this.dialog.open(ExerciseDetailDialogComponent, { data: ex.id, width: '640px', maxWidth: '95vw' });
  }

  regenerate(): void {
    if (!confirm('Rebuild your plan from your current assessment? Your workout history is kept.')) return;
    this.busy = true;
    this.workouts.regenerate().pipe(finalize(() => (this.busy = false))).subscribe((p) => {
      this.plan = p;
      this.notify.success('Your plan has been rebuilt');
    });
  }

  swap(e: PlanExercise): void {
    this.dialog.open<AlternativesDialogComponent, AlternativesDialogData, ExerciseSummary>(AlternativesDialogComponent, {
      data: { exercise: e.exercise, mode: 'plan' }, width: '520px', maxWidth: '95vw',
    }).afterClosed().subscribe((alt) => {
      if (!alt) return;
      this.workouts.swapExercise(e.id, alt.id).subscribe((updated) => {
        Object.assign(e, updated);
        this.notify.success(`${alt.name} is now in your plan`);
      });
    });
  }
}
