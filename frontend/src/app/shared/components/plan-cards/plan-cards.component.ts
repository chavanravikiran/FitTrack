import { Component, EventEmitter, Input, Output } from '@angular/core';
import { BillingCycle, Plan } from '../../../core/models/billing.models';
import { CORE_INCLUDED, featureLine } from '../../constants/features';

/**
 * Pricing cards. In 'public' mode the CTA links to sign-up; in 'app' mode it emits (choose)
 * so the billing page can start a checkout.
 */
@Component({
  selector: 'ft-plan-cards',
  standalone: false,
  templateUrl: './plan-cards.component.html',
  styleUrl: './plan-cards.component.scss',
})
export class PlanCardsComponent {
  @Input({ required: true }) plans: Plan[] = [];
  @Input() cycle: BillingCycle = 'MONTHLY';
  @Input() mode: 'public' | 'app' = 'public';
  @Input() currentPlanCode: string | null = null;
  /** True when the user's current paid plan is time-limited and can be extended. */
  @Input() canRenew = false;
  @Input() trialDays = 0;
  @Input() busyPlanCode: string | null = null;
  @Input() paymentsEnabled = true;
  @Output() choose = new EventEmitter<Plan>();

  readonly core = CORE_INCLUDED;
  readonly line = featureLine;

  price(plan: Plan): number {
    return this.cycle === 'YEARLY' ? plan.priceYearlyMinor : plan.priceMonthlyMinor;
  }

  /** Effective monthly price when billed yearly, for comparison. */
  perMonth(plan: Plan): number {
    return Math.round(plan.priceYearlyMinor / 12);
  }

  savingsPct(plan: Plan): number {
    const fullYear = plan.priceMonthlyMinor * 12;
    return fullYear > 0 && plan.priceYearlyMinor > 0 ? Math.round((1 - plan.priceYearlyMinor / fullYear) * 100) : 0;
  }

  isCurrent(plan: Plan): boolean {
    return this.currentPlanCode === plan.code;
  }

  ctaLabel(plan: Plan): string {
    if (this.mode === 'public') {
      return plan.free ? 'Get started free' : this.trialDays > 0 ? `Start ${this.trialDays}-day free trial` : 'Get Premium';
    }
    if (plan.free) return this.isCurrent(plan) ? 'Current plan' : 'Included';
    if (this.isCurrent(plan)) return this.canRenew ? 'Extend' : 'Upgrade';
    return 'Upgrade';
  }

  ctaDisabled(plan: Plan): boolean {
    return this.mode === 'app' && (plan.free || !this.paymentsEnabled || this.busyPlanCode !== null);
  }
}
