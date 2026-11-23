import { Component, OnInit } from '@angular/core';
import { AdminStats, DayCount } from '../../../core/models/admin.models';
import { AdminService } from '../../../core/services/admin.service';

@Component({
  selector: 'ft-admin-overview',
  standalone: false,
  template: `
    @if (stats) {
      @if (!stats.paymentsEnabled) {
        <div class="alert" role="status">
          <mat-icon aria-hidden="true">info</mat-icon>
          Online payments are off. Set RAZORPAY_KEY_ID, RAZORPAY_KEY_SECRET and RAZORPAY_WEBHOOK_SECRET on the server to enable checkout.
          You can still grant Premium from the Users tab.
        </div>
      }
      <section class="ft-grid">
        <ft-stat-card label="Total users" [value]="stats.totalUsers" icon="group" tone="primary" [hint]="'+' + stats.newUsersLast30Days + ' in the last 30 days'" />
        <ft-stat-card label="Active (30 days)" [value]="stats.activeUsersLast30Days" icon="bolt" tone="secondary" hint="Signed in recently" />
        <ft-stat-card label="Premium" [value]="stats.premiumUsers" icon="workspace_premium" tone="tertiary" [hint]="stats.trialingUsers + ' on free trial'" />
        <ft-stat-card label="MRR (est.)" [value]="stats.mrrMinor | money: stats.currency" icon="trending_up" tone="primary" hint="Monthly recurring revenue" />
        <ft-stat-card label="Revenue (30 days)" [value]="stats.revenueLast30DaysMinor | money: stats.currency" icon="payments" tone="secondary" />
        <ft-stat-card label="Conversion" [value]="conversion" unit="%" icon="percent" tone="neutral" hint="Premium ÷ total users" />
      </section>

      <mat-card appearance="outlined" class="ft-card signups">
        <h2 class="ft-section-title">Sign-ups, last 30 days</h2>
        @if (days.length) {
          <div class="bars" role="img" [attr.aria-label]="'Daily sign-ups over the last 30 days, total ' + stats.newUsersLast30Days">
            @for (d of days; track d.date) {
              <div class="bar" [style.height.%]="max ? (d.count / max) * 100 : 0" [matTooltip]="(d.date | date: 'mediumDate') + ': ' + d.count">
              </div>
            }
          </div>
          <div class="axis ft-muted"><span>{{ days[0].date | date: 'd MMM' }}</span><span>{{ days[days.length - 1].date | date: 'd MMM' }}</span></div>
        } @else {
          <p class="ft-muted">No sign-ups in this period yet.</p>
        }
      </mat-card>
      <p class="ft-muted small">MRR counts active paid subscriptions at current plan prices (yearly ÷ 12). Admin grants are excluded.</p>
    } @else {
      <mat-progress-bar mode="indeterminate" />
    }
  `,
  styles: `
    .alert { display: flex; gap: 10px; align-items: flex-start; padding: 12px 14px; border-radius: 12px; margin-bottom: 16px;
             background: var(--mat-sys-tertiary-container); color: var(--mat-sys-on-tertiary-container); }
    .signups { margin-top: 16px; }
    .bars { display: flex; align-items: flex-end; gap: 4px; height: 160px; padding-top: 8px; }
    .bar { flex: 1; min-height: 2px; border-radius: 4px 4px 0 0; background: var(--mat-sys-primary); opacity: .85; }
    .bar:hover { opacity: 1; }
    .axis { display: flex; justify-content: space-between; font: var(--mat-sys-body-small); margin-top: 6px; }
    .small { font: var(--mat-sys-body-small); margin-top: 12px; }
  `,
})
export class AdminOverviewComponent implements OnInit {
  stats: AdminStats | null = null;
  days: DayCount[] = [];
  max = 0;

  constructor(private readonly admin: AdminService) {}

  ngOnInit(): void {
    this.admin.stats().subscribe((s) => {
      this.stats = s;
      this.days = this.fillDays(s.signupsLast30Days);
      this.max = Math.max(0, ...this.days.map((d) => d.count));
    });
  }

  get conversion(): number | null {
    const s = this.stats;
    return s && s.totalUsers ? Math.round((s.premiumUsers / s.totalUsers) * 1000) / 10 : null;
  }

  /** The API returns only days with sign-ups; fill gaps with zero so bars line up with the calendar. */
  private fillDays(data: DayCount[]): DayCount[] {
    const byDate = new Map(data.map((d) => [d.date, d.count]));
    const out: DayCount[] = [];
    const today = new Date();
    for (let i = 29; i >= 0; i--) {
      const d = new Date(Date.UTC(today.getUTCFullYear(), today.getUTCMonth(), today.getUTCDate() - i));
      const key = d.toISOString().substring(0, 10);
      out.push({ date: key, count: byDate.get(key) ?? 0 });
    }
    return out;
  }
}
