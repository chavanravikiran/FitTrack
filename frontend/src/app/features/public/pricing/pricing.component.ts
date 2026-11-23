import { Component, OnInit } from '@angular/core';
import { catchError, of } from 'rxjs';
import { BillingConfig, BillingCycle, Plan } from '../../../core/models/billing.models';
import { BillingService } from '../../../core/services/billing.service';

@Component({
  selector: 'ft-pricing',
  standalone: false,
  template: `
    <section class="wrap">
      <h1>Pricing</h1>
      <p class="lead ft-muted">Start free. Upgrade when you want more insight and tools. Prices in INR, taxes as applicable.</p>
      <mat-button-toggle-group [(value)]="cycle" aria-label="Billing period" class="cycle">
        <mat-button-toggle value="MONTHLY">Monthly</mat-button-toggle>
        <mat-button-toggle value="YEARLY">Yearly</mat-button-toggle>
      </mat-button-toggle-group>
      @if (plans.length) {
        <ft-plan-cards [plans]="plans" [cycle]="cycle" mode="public" [trialDays]="config?.trialDays ?? 0" />
      } @else if (loaded) {
        <ft-empty-state icon="cloud_off" title="Pricing is unavailable right now" message="Please try again in a moment." />
      } @else {
        <mat-progress-bar mode="indeterminate" />
      }
      <p class="ft-muted note">
        Payments are processed securely by Razorpay. Plans don't renew automatically - you choose when to renew.
        See our <a routerLink="/terms">Terms</a> for refunds and cancellations.
      </p>
    </section>
  `,
  styles: `
    .wrap { max-width: 1000px; margin: 0 auto; padding: 48px 16px 64px; text-align: center; }
    h1 { margin: 0 0 8px; font: var(--mat-sys-headline-large); font-weight: 800; }
    .lead { margin: 0 0 20px; }
    .cycle { margin-bottom: 28px; }
    ft-plan-cards { display: block; text-align: left; }
    .note { margin-top: 24px; font: var(--mat-sys-body-small); }
  `,
})
export class PricingComponent implements OnInit {
  plans: Plan[] = [];
  config: BillingConfig | null = null;
  cycle: BillingCycle = 'MONTHLY';
  loaded = false;

  constructor(private readonly billing: BillingService) {}

  ngOnInit(): void {
    this.billing.plans().pipe(catchError(() => of([] as Plan[]))).subscribe((p) => {
      this.plans = p;
      this.loaded = true;
    });
    this.billing.config().pipe(catchError(() => of(null))).subscribe((c) => (this.config = c));
  }
}
