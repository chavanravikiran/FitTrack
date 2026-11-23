import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { Recovery, SleepEntry } from '../../../core/models/activity.models';
import { ActivityService } from '../../../core/services/activity.service';
import { NotifyService } from '../../../core/services/notify.service';
import { BarDatum } from '../../../shared/components/bar-chart.component';
import { formatDuration, shortDate, shortDay } from '../../../shared/constants/labels';
import { toIsoDate } from '../../../shared/forms/validators';

const HHMM = /^([01]\d|2[0-3]):[0-5]\d$/;

@Component({
  selector: 'ft-sleep',
  standalone: false,
  template: `
    <div class="layout">
      <mat-card appearance="outlined" class="ft-card">
        <h2 class="ft-section-title">Log last night</h2>
        <form [formGroup]="form" (ngSubmit)="save()" class="ft-form" novalidate>
          <mat-form-field appearance="outline">
            <mat-label>Morning I woke up</mat-label>
            <input matInput [matDatepicker]="dp" formControlName="date" [max]="maxDate" />
            <mat-datepicker-toggle matIconSuffix [for]="dp" />
            <mat-datepicker #dp />
          </mat-form-field>
          <div class="row">
            <mat-form-field appearance="outline">
              <mat-label>Went to bed</mat-label>
              <input matInput type="time" formControlName="bedTime" />
              <mat-error>Enter a time</mat-error>
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Woke up</mat-label>
              <input matInput type="time" formControlName="wakeTime" />
              <mat-error>Enter a time</mat-error>
            </mat-form-field>
          </div>
          <p class="duration">Sleep: <strong>{{ preview }}</strong></p>
          <label class="label" id="q-label">Sleep quality</label>
          <div class="stars" role="radiogroup" aria-labelledby="q-label">
            @for (n of [1, 2, 3, 4, 5]; track n) {
              <button type="button" mat-icon-button role="radio" [attr.aria-checked]="form.controls.quality.value === n"
                      [attr.aria-label]="n + ' out of 5'" (click)="form.controls.quality.setValue(n)">
                <mat-icon>{{ (form.controls.quality.value ?? 0) >= n ? 'star' : 'star_border' }}</mat-icon>
              </button>
            }
          </div>
          <mat-checkbox formControlName="restDay">Today is a rest day</mat-checkbox>
          <mat-form-field appearance="outline" class="notes">
            <mat-label>Notes (optional)</mat-label>
            <input matInput formControlName="notes" maxlength="255" placeholder="e.g. woke up twice, late coffee" />
          </mat-form-field>
          <button mat-flat-button type="submit" [disabled]="saving">Save sleep</button>
        </form>
      </mat-card>

      <div class="side">
        @if (recovery) {
          <mat-card appearance="outlined" class="ft-card recovery" [class]="'rec--' + recovery.status.toLowerCase()">
            <h2 class="ft-section-title"><mat-icon inline aria-hidden="true">battery_charging_full</mat-icon> Recovery</h2>
            <div class="rec-grid">
              <ft-stat-card label="Avg sleep (7 days)" [value]="recovery.averageSleepHours7d ?? '—'" unit="h" icon="bedtime" tone="primary"
                            [hint]="'Goal ' + recovery.sleepGoalHours + ' h'" />
              <ft-stat-card label="Workouts (7 days)" [value]="recovery.workoutsLast7Days" icon="fitness_center" tone="secondary"
                            [hint]="recovery.averageEffort7d ? 'avg effort ' + recovery.averageEffort7d + '/10' : undefined" />
            </div>
            <p class="intensity">Suggested intensity today: <strong>{{ recovery.suggestedIntensity }}</strong></p>
            <ul class="tips">@for (t of recovery.tips; track t) { <li>{{ t }}</li> }</ul>
          </mat-card>
        }
        <mat-card appearance="outlined" class="ft-card">
          <h2 class="ft-section-title">Last 14 nights</h2>
          @if (history.length) {
            <ft-bar-chart [data]="chart" unit="h" ariaLabel="Hours of sleep per night" />
            <ul class="list">
              @for (s of history.slice().reverse(); track s.date) {
                <li>
                  <span>{{ s.date | date: 'EEE d MMM' }}</span>
                  <span class="ft-muted">{{ s.bedTimeLocal }} → {{ s.wakeTimeLocal }}</span>
                  <strong>{{ dur(s.durationMin) }}</strong>
                  <button mat-icon-button (click)="remove(s.date)" [attr.aria-label]="'Delete sleep for ' + s.date"><mat-icon>delete</mat-icon></button>
                </li>
              }
            </ul>
          } @else {
            <p class="ft-muted">No sleep logged yet.</p>
          }
        </mat-card>
      </div>
    </div>
  `,
  styles: `
    .layout { display: grid; grid-template-columns: 1fr; gap: 16px; }
    @media (min-width: 1000px) { .layout { grid-template-columns: 380px 1fr; align-items: start; } }
    .row { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
    .duration { margin: 0 0 8px; }
    .label { font: var(--mat-sys-title-small); }
    .stars { display: flex; margin-bottom: 8px; }
    .stars mat-icon { color: var(--mat-sys-tertiary); }
    .notes { margin-top: 12px; }
    .side { display: flex; flex-direction: column; gap: 16px; }
    .rec-grid { display: grid; gap: 12px; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); }
    .rec--low { border-color: var(--mat-sys-error); }
    .rec--moderate { border-color: var(--mat-sys-tertiary); }
    .intensity { margin: 16px 0 4px; }
    .tips { margin: 0; padding-left: 20px; color: var(--mat-sys-on-surface-variant); font: var(--mat-sys-body-small); }
    .list { list-style: none; padding: 0; margin: 12px 0 0; }
    .list li { display: grid; grid-template-columns: 1fr 1fr auto auto; gap: 8px; align-items: center; border-bottom: 1px solid var(--mat-sys-outline-variant); }
  `,
})
export class SleepComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  history: SleepEntry[] = [];
  chart: BarDatum[] = [];
  recovery: Recovery | null = null;
  saving = false;
  readonly maxDate = new Date();
  readonly form = this.fb.group({
    date: this.fb.nonNullable.control<Date>(new Date(), Validators.required),
    bedTime: this.fb.nonNullable.control('22:30', [Validators.required, Validators.pattern(HHMM)]),
    wakeTime: this.fb.nonNullable.control('06:30', [Validators.required, Validators.pattern(HHMM)]),
    quality: this.fb.control<number | null>(3),
    restDay: this.fb.nonNullable.control(false),
    notes: this.fb.control<string | null>(null),
  });
  readonly dur = formatDuration;

  constructor(
    private readonly activity: ActivityService,
    private readonly notify: NotifyService,
  ) {}

  get preview(): string {
    const { bedTime, wakeTime } = this.form.getRawValue();
    if (!HHMM.test(bedTime) || !HHMM.test(wakeTime)) return '—';
    const [bh, bm] = bedTime.split(':').map(Number);
    const [wh, wm] = wakeTime.split(':').map(Number);
    let mins = wh * 60 + wm - (bh * 60 + bm);
    if (mins <= 0) mins += 24 * 60;
    return formatDuration(mins);
  }

  ngOnInit(): void {
    this.load();
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    this.saving = true;
    this.activity.saveSleep(toIsoDate(v.date)!, { bedTime: v.bedTime, wakeTime: v.wakeTime, quality: v.quality, restDay: v.restDay, notes: v.notes?.trim() || null })
      .pipe(finalize(() => (this.saving = false)))
      .subscribe(() => {
        this.notify.success('Sleep saved');
        this.load();
      });
  }

  remove(date: string): void {
    this.activity.deleteSleep(date).subscribe(() => this.load());
  }

  private load(): void {
    this.activity.sleepHistory(14).subscribe((h) => {
      this.history = h;
      this.chart = h.map((s) => ({ label: shortDay(s.date), value: Math.round((s.durationMin / 60) * 10) / 10, goal: this.recovery?.sleepGoalHours,
        tooltip: `${shortDate(s.date)}: ${formatDuration(s.durationMin)}` }));
    });
    this.activity.recovery().subscribe((r) => {
      this.recovery = r;
      this.chart = this.chart.map((c) => ({ ...c, goal: r.sleepGoalHours }));
    });
  }
}
