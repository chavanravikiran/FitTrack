import { Component, OnInit } from '@angular/core';
import { WaterDay } from '../../../core/models/activity.models';
import { ActivityService } from '../../../core/services/activity.service';
import { NotifyService } from '../../../core/services/notify.service';
import { BarDatum } from '../../../shared/components/bar-chart.component';
import { shortDate, shortDay } from '../../../shared/constants/labels';

@Component({
  selector: 'ft-water',
  standalone: false,
  template: `
    @if (day) {
      <div class="layout">
        <mat-card appearance="outlined" class="ft-card main">
          <ft-progress-ring [percent]="day.percent" label="Water toward today's goal" [size]="190">
            <mat-icon class="drop" aria-hidden="true">water_drop</mat-icon>
            <span class="big">{{ day.totalMl | number }} ml</span>
            <span class="ft-muted small">of {{ day.goalMl | number }} ml</span>
          </ft-progress-ring>
          <p class="remaining" role="status">
            @if (day.remainingMl > 0) { {{ day.remainingMl | number }} ml to go } @else { Goal reached - well done! }
          </p>
          <div class="quick">
            <button mat-flat-button (click)="add(250)" [disabled]="busy"><mat-icon>local_drink</mat-icon> +250 ml</button>
            <button mat-flat-button (click)="add(500)" [disabled]="busy"><mat-icon>water_full</mat-icon> +500 ml</button>
          </div>
          <div class="custom">
            <mat-form-field appearance="outline" subscriptSizing="dynamic">
              <mat-label>Custom (ml)</mat-label>
              <input matInput type="number" min="1" max="3000" [(ngModel)]="custom" (keyup.enter)="addCustom()" />
            </mat-form-field>
            <button mat-stroked-button (click)="addCustom()" [disabled]="busy || !custom">Add</button>
          </div>

          @if (day.entries.length) {
            <h3>Today's log</h3>
            <ul class="entries">
              @for (e of day.entries.slice().reverse(); track e.id) {
                <li>
                  <span>{{ e.loggedAt | date: 'shortTime' }}</span>
                  <strong>{{ e.amountMl }} ml</strong>
                  <button mat-icon-button (click)="remove(e.id)" [attr.aria-label]="'Remove ' + e.amountMl + ' ml entry'"><mat-icon>close</mat-icon></button>
                </li>
              }
            </ul>
          }
        </mat-card>

        <div class="side">
          <mat-card appearance="outlined" class="ft-card">
            <h2 class="ft-section-title">Daily goal</h2>
            <p class="ft-muted">{{ day.customGoal ? 'Adjusted for today' + (day.goalReason ? ': ' + day.goalReason : '') : 'Personalised from your weight and activity.' }}</p>
            <div class="goal-row">
              <mat-form-field appearance="outline" subscriptSizing="dynamic">
                <mat-label>Goal (ml)</mat-label>
                <input matInput type="number" min="500" max="6000" step="250" [(ngModel)]="goal" />
              </mat-form-field>
              <mat-form-field appearance="outline" subscriptSizing="dynamic">
                <mat-label>Reason</mat-label>
                <mat-select [(ngModel)]="reason">
                  @for (r of reasons; track r) { <mat-option [value]="r">{{ r }}</mat-option> }
                </mat-select>
              </mat-form-field>
              <button mat-stroked-button (click)="saveGoal()" [disabled]="!goal">Update</button>
            </div>
            <ul class="guidance">
              @for (g of day.guidance; track g) { <li>{{ g }}</li> }
            </ul>
          </mat-card>

          <mat-card appearance="outlined" class="ft-card">
            <h2 class="ft-section-title">Last 7 days</h2>
            <ft-bar-chart [data]="chart" unit="ml" ariaLabel="Water per day for the last 7 days" />
          </mat-card>
        </div>
      </div>
    } @else {
      <mat-progress-bar mode="indeterminate" />
    }
  `,
  styles: `
    .layout { display: grid; grid-template-columns: 1fr; gap: 16px; }
    @media (min-width: 1000px) { .layout { grid-template-columns: 380px 1fr; align-items: start; } }
    .main { display: flex; flex-direction: column; align-items: center; }
    .drop { color: var(--mat-sys-tertiary); }
    .big { font: var(--mat-sys-headline-small); font-weight: 700; font-variant-numeric: tabular-nums; }
    .small { font: var(--mat-sys-body-small); }
    .remaining { font: var(--mat-sys-title-medium); margin: 12px 0; }
    .quick { display: flex; gap: 10px; }
    .quick button { height: 48px; }
    .custom { display: flex; gap: 8px; align-items: center; margin-top: 12px; }
    h3 { align-self: flex-start; font: var(--mat-sys-title-small); margin: 20px 0 6px; }
    .entries { list-style: none; padding: 0; margin: 0; width: 100%; }
    .entries li { display: grid; grid-template-columns: 1fr auto auto; align-items: center; gap: 8px; border-bottom: 1px solid var(--mat-sys-outline-variant); }
    .side { display: flex; flex-direction: column; gap: 16px; }
    .goal-row { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; margin-bottom: 8px; }
    .guidance { margin: 8px 0 0; padding-left: 20px; color: var(--mat-sys-on-surface-variant); font: var(--mat-sys-body-small); }
    .guidance li { margin-bottom: 4px; }
  `,
})
export class WaterComponent implements OnInit {
  day: WaterDay | null = null;
  chart: BarDatum[] = [];
  custom: number | null = null;
  goal: number | null = null;
  reason = 'Hot weather';
  busy = false;
  readonly reasons = ['Hot weather', 'Long workout', 'Feeling unwell', 'Other'];

  constructor(
    private readonly activity: ActivityService,
    private readonly notify: NotifyService,
  ) {}

  ngOnInit(): void {
    this.activity.water().subscribe((d) => this.setDay(d));
    this.loadHistory();
  }

  add(ml: number): void {
    this.busy = true;
    this.activity.addWater(ml).subscribe({
      next: (d) => {
        const wasBelow = (this.day?.percent ?? 0) < 100;
        this.setDay(d);
        this.busy = false;
        if (wasBelow && d.percent >= 100) this.notify.success('Hydration goal reached!');
        this.loadHistory();
      },
      error: () => (this.busy = false),
    });
  }

  addCustom(): void {
    if (this.custom && this.custom > 0 && this.custom <= 3000) {
      this.add(Math.round(this.custom));
      this.custom = null;
    }
  }

  remove(id: number): void {
    this.activity.deleteWater(id).subscribe((d) => {
      this.setDay(d);
      this.loadHistory();
    });
  }

  saveGoal(): void {
    if (!this.day || !this.goal) return;
    this.activity.setWaterGoal(this.day.date, Math.round(this.goal), this.reason).subscribe((d) => {
      this.setDay(d);
      this.notify.success('Goal updated for today');
      this.loadHistory();
    });
  }

  private setDay(d: WaterDay): void {
    this.day = d;
    this.goal = d.goalMl;
  }

  private loadHistory(): void {
    this.activity.waterHistory(7).subscribe((h) => {
      this.chart = h.map((d) => ({ label: shortDay(d.date), value: d.totalMl, goal: d.goalMl, tooltip: `${shortDate(d.date)}: ${d.totalMl} / ${d.goalMl} ml` }));
    });
  }
}
