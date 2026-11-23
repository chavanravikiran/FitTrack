import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { ActivitySummary } from '../../../core/models/activity.models';
import { ActivityService } from '../../../core/services/activity.service';
import { NotifyService } from '../../../core/services/notify.service';
import { BarDatum } from '../../../shared/components/bar-chart.component';
import { shortDate, shortDay } from '../../../shared/constants/labels';
import { toIsoDate } from '../../../shared/forms/validators';

@Component({
  selector: 'ft-steps',
  standalone: false,
  template: `
    <div class="layout">
      <mat-card appearance="outlined" class="ft-card today">
        <h2 class="ft-section-title">Log steps</h2>
        @if (summary) {
          <ft-progress-ring [percent]="pct" label="Steps toward today's goal">
            <span class="big">{{ summary.today.steps | number }}</span>
            <span class="ft-muted small">of {{ summary.stepGoal | number }}</span>
          </ft-progress-ring>
        }
        <form [formGroup]="form" (ngSubmit)="save()" class="ft-form form" novalidate>
          <mat-form-field appearance="outline">
            <mat-label>Date</mat-label>
            <input matInput [matDatepicker]="dp" formControlName="date" [max]="maxDate" />
            <mat-datepicker-toggle matIconSuffix [for]="dp" />
            <mat-datepicker #dp />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Steps</mat-label>
            <input matInput type="number" formControlName="steps" min="0" max="100000" inputmode="numeric" />
            <mat-hint>Copy today's count from your phone's health app or pedometer</mat-hint>
            <mat-error>{{ form.controls.steps | formError: 'Steps' }}</mat-error>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Active minutes (optional)</mat-label>
            <input matInput type="number" formControlName="activeMinutes" min="0" max="1440" />
          </mat-form-field>
          <button mat-flat-button type="submit" [disabled]="saving">Save steps</button>
        </form>
        @if (summary?.today?.distanceM) {
          <p class="ft-muted small">≈ {{ (summary!.today.distanceM! / 1000) | number: '1.1-1' }} km · ~{{ summary!.today.caloriesEst }} kcal (estimates)</p>
        }
        <p class="ft-muted tiny">FitTrack doesn't count steps automatically in the browser. Phone health-app sync (Google Health Connect / Apple Health) needs our future mobile app.</p>
      </mat-card>

      <mat-card appearance="outlined" class="ft-card">
        <div class="head">
          <h2 class="ft-section-title">History</h2>
          <mat-button-toggle-group [value]="days" (change)="setDays($event.value)" aria-label="Period">
            <mat-button-toggle [value]="7">Week</mat-button-toggle>
            <mat-button-toggle [value]="30">Month</mat-button-toggle>
          </mat-button-toggle-group>
        </div>
        @if (summary) {
          <ft-bar-chart [data]="chart" unit="steps" [showLabels]="days === 7" ariaLabel="Daily steps" />
          <div class="kpis">
            <ft-stat-card label="Average" [value]="(summary.averageSteps | number) ?? '0'" icon="equalizer" tone="primary" hint="On days you logged" />
            <ft-stat-card label="Goal reached" [value]="summary.goalMetDays + ' / ' + summary.days" icon="flag" tone="tertiary" hint="days" />
            <ft-stat-card label="Total" [value]="(summary.totalSteps | number) ?? '0'" icon="directions_walk" tone="secondary" />
          </div>
          <p class="insight"><mat-icon inline aria-hidden="true">lightbulb</mat-icon> {{ summary.insight }}</p>
        } @else {
          <mat-progress-bar mode="indeterminate" />
        }
      </mat-card>
    </div>
  `,
  styles: `
    .layout { display: grid; grid-template-columns: 1fr; gap: 16px; }
    @media (min-width: 1000px) { .layout { grid-template-columns: 340px 1fr; align-items: start; } }
    .today { display: flex; flex-direction: column; align-items: center; }
    .today .ft-section-title { align-self: flex-start; }
    .form { width: 100%; margin-top: 16px; }
    .big { font: var(--mat-sys-headline-medium); font-weight: 700; font-variant-numeric: tabular-nums; }
    .small { font: var(--mat-sys-body-small); }
    .tiny { font: var(--mat-sys-body-small); margin-top: 12px; }
    .head { display: flex; justify-content: space-between; align-items: center; gap: 12px; flex-wrap: wrap; margin-bottom: 12px; }
    .head .ft-section-title { margin: 0; }
    .kpis { display: grid; gap: 12px; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); margin-top: 16px; }
    .insight { display: flex; gap: 6px; margin: 16px 0 0; color: var(--mat-sys-on-surface-variant); }
  `,
})
export class StepsComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  summary: ActivitySummary | null = null;
  chart: BarDatum[] = [];
  days = 7;
  saving = false;
  readonly maxDate = new Date();
  readonly form = this.fb.group({
    date: this.fb.nonNullable.control<Date>(new Date(), Validators.required),
    steps: this.fb.control<number | null>(null, [Validators.required, Validators.min(0), Validators.max(100000)]),
    activeMinutes: this.fb.control<number | null>(null, [Validators.min(0), Validators.max(1440)]),
  });

  constructor(
    private readonly activity: ActivityService,
    private readonly notify: NotifyService,
  ) {}

  get pct(): number {
    const s = this.summary;
    return s && s.stepGoal ? Math.round((s.today.steps / s.stepGoal) * 100) : 0;
  }

  ngOnInit(): void {
    this.load();
  }

  setDays(d: number): void {
    this.days = d;
    this.load();
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    this.saving = true;
    this.activity.saveSteps(toIsoDate(v.date)!, Number(v.steps), null, v.activeMinutes)
      .pipe(finalize(() => (this.saving = false)))
      .subscribe((d) => {
        this.notify.success(d.goalMet ? 'Goal reached - fantastic!' : 'Steps saved');
        this.load();
      });
  }

  private load(): void {
    this.activity.stepSummary(this.days).subscribe((s) => {
      this.summary = s;
      if (!this.form.controls.steps.value && s.today.steps) this.form.controls.steps.setValue(s.today.steps);
      this.chart = s.history.map((h) => ({
        label: this.days === 7 ? shortDay(h.date) : shortDate(h.date),
        value: h.steps,
        goal: h.stepGoal,
        tooltip: `${shortDate(h.date)}: ${h.steps.toLocaleString()} steps`,
      }));
    });
  }
}
