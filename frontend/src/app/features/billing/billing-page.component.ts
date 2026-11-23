import { Component, OnInit } from '@angular/core';
import { catchError, finalize, forkJoin, of, switchMap } from 'rxjs';
import { BillingConfig, BillingCycle, Payment, Plan, Subscription } from '../../core/models/billing.models';
import { AuthService } from '../../core/services/auth.service';
import { BillingService } from '../../core/services/billing.service';
import { NotifyService } from '../../core/services/notify.service';
import { RazorpayService } from '../../core/services/razorpay.service';
import { apiErrorMessage } from '../../core/utils/api-error';
import { featureLine } from '../../shared/constants/features';

const BRAND_COLOR = '#2f6b3c';

@Component({
  selector: 'ft-billing-page',
  standalone: false,
  templateUrl: './billing-page.component.html',
  styleUrl: './billing-page.component.scss',
})
export class BillingPageComponent implements OnInit {
  subscription: Subscription | null = null;
  plans: Plan[] = [];
  config: BillingConfig | null = null;
  payments: Payment[] = [];
  cycle: BillingCycle = 'YEARLY';
  busyPlanCode: string | null = null;
  error: string | null = null;
  resending = false;

  readonly line = featureLine;
  readonly paymentColumns = ['date', 'plan', 'amount', 'status', 'reference'];

  constructor(
    readonly auth: AuthService,
    private readonly billing: BillingService,
    private readonly razorpay: RazorpayService,
    private readonly notify: NotifyService,
  ) {}

  ngOnInit(): void {
    forkJoin({
      sub: this.billing.loadSubscription(),
      plans: this.billing.plans(),
      config: this.billing.config(),
    }).subscribe(({ sub, plans, config }) => {
      this.subscription = sub;
      this.plans = plans;
      this.config = config;
    });
    this.loadPayments();
  }

  get canRenew(): boolean {
    const s = this.subscription;
    return !!s && s.premium && (s.source === 'PAYMENT' || s.source === 'ADMIN_GRANT');
  }

  get statusLabel(): string {
    const s = this.subscription;
    if (!s) return '';
    if (s.status === 'TRIALING') return 'Free trial';
    if (s.source === 'ADMIN_GRANT') return 'Complimentary';
    return s.premium ? 'Active' : 'Free plan';
  }

  upgrade(plan: Plan): void {
    if (!this.auth.currentUser?.emailVerified) {
      this.error = 'Please verify your email address before upgrading, so we can send your receipts.';
      return;
    }
    this.busyPlanCode = plan.code;
    this.error = null;
    this.billing
      .checkout(plan.code, this.cycle)
      .pipe(
        switchMap((order) => this.razorpay.pay(order, BRAND_COLOR)),
        switchMap((result) => this.billing.verify(result.razorpay_order_id, result.razorpay_payment_id, result.razorpay_signature)),
        finalize(() => (this.busyPlanCode = null)),
      )
      .subscribe({
        next: (sub) => {
          this.subscription = sub;
          this.notify.success(`Payment successful - ${sub.planName} is active until ${new Date(sub.currentPeriodEnd!).toLocaleDateString()}.`);
          this.loadPayments();
        },
        error: (err) => (this.error = err instanceof Error ? err.message : apiErrorMessage(err, 'Payment could not be completed.')),
        // Completing without a value means the user closed the payment window - nothing to do.
      });
  }

  resendVerification(): void {
    this.resending = true;
    this.auth
      .resendVerification()
      .pipe(finalize(() => (this.resending = false)))
      .subscribe((msg) => this.notify.success(msg));
  }

  private loadPayments(): void {
    this.billing
      .payments(0, 20)
      .pipe(catchError(() => of(null)))
      .subscribe((page) => (this.payments = page?.content ?? []));
  }
}
