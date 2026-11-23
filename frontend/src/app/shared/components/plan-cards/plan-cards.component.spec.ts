import { Plan } from '../../../core/models/billing.models';
import { PlanCardsComponent } from './plan-cards.component';

describe('PlanCardsComponent', () => {
  const premium: Plan = {
    code: 'PREMIUM', name: 'Premium', description: null, priceMonthlyMinor: 19900, priceYearlyMinor: 199900,
    currency: 'INR', free: false, active: true, features: [],
  };
  const free: Plan = { ...premium, code: 'FREE', name: 'Free', priceMonthlyMinor: 0, priceYearlyMinor: 0, free: true };

  it('computes yearly savings and per-month price', () => {
    const c = new PlanCardsComponent();
    expect(c.savingsPct(premium)).toBe(16); // 1999 vs 12 x 199 = 2388
    expect(c.perMonth(premium)).toBe(16658);
  });

  it('labels CTAs by mode and current plan', () => {
    const c = new PlanCardsComponent();
    c.trialDays = 7;
    expect(c.ctaLabel(premium)).toBe('Start 7-day free trial');
    c.mode = 'app';
    c.currentPlanCode = 'PREMIUM';
    c.canRenew = true;
    expect(c.ctaLabel(premium)).toBe('Extend');
    expect(c.ctaDisabled(free)).toBeTrue();
  });
});
