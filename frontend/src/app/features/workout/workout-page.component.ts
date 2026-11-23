import { Component, Input, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, of } from 'rxjs';
import { WorkoutSummary } from '../../core/models/workout.models';
import { WorkoutService } from '../../core/services/workout.service';

const TABS = ['today', 'week', 'plan', 'history'] as const;

@Component({
  selector: 'ft-workout-page',
  standalone: false,
  template: `
    <ft-page-header title="My Workout" icon="fitness_center" subtitle="Your personalised plan, today's session and your progress.">
      @if (summary) {
        <div class="stats">
          <span class="stat"><mat-icon aria-hidden="true">event_available</mat-icon>{{ summary.completedThisWeek }}/{{ summary.plannedThisWeek }} this week</span>
          <span class="stat"><mat-icon aria-hidden="true">local_fire_department</mat-icon>{{ summary.streakDays }}-workout streak</span>
        </div>
      }
    </ft-page-header>
    <mat-tab-group [selectedIndex]="index" (selectedIndexChange)="select($event)" mat-stretch-tabs="false" animationDuration="150ms">
      <mat-tab label="Today"><ng-template matTabContent><ft-workout-today [date]="date" [planDayId]="planDayId" (changed)="loadSummary()" /></ng-template></mat-tab>
      <mat-tab label="Calendar"><ng-template matTabContent><ft-workout-week (changed)="loadSummary()" /></ng-template></mat-tab>
      <mat-tab label="My plan"><ng-template matTabContent><ft-workout-plan /></ng-template></mat-tab>
      <mat-tab label="History"><ng-template matTabContent><ft-workout-history /></ng-template></mat-tab>
    </mat-tab-group>
  `,
  styles: `
    .stats { display: flex; gap: 8px; flex-wrap: wrap; }
    .stat { display: inline-flex; align-items: center; gap: 6px; padding: 6px 12px; border-radius: 999px;
            background: var(--mat-sys-surface-container); font: var(--mat-sys-label-large); }
    .stat mat-icon { font-size: 18px; width: 18px; height: 18px; color: var(--mat-sys-primary); }
    ::ng-deep .mat-mdc-tab-body-content { padding-top: 20px; }
  `,
})
export class WorkoutPageComponent implements OnInit {
  index = 0;
  summary: WorkoutSummary | null = null;
  @Input() date?: string;
  @Input() planDayId?: string;

  @Input() set tab(value: string | undefined) {
    const i = TABS.indexOf((value ?? 'today') as (typeof TABS)[number]);
    this.index = i >= 0 ? i : 0;
  }

  constructor(
    private readonly router: Router,
    private readonly workouts: WorkoutService,
  ) {}

  ngOnInit(): void {
    this.loadSummary();
  }

  loadSummary(): void {
    this.workouts.summary().pipe(catchError(() => of(null))).subscribe((s) => (this.summary = s));
  }

  select(i: number): void {
    this.index = i;
    this.router.navigate([], { queryParams: { tab: TABS[i] }, queryParamsHandling: i === 0 ? 'merge' : '', replaceUrl: true });
  }
}
