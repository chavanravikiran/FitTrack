export type Feature =
  | 'ACTIVE_GOALS'
  | 'ADVANCED_ANALYTICS'
  | 'PDF_REPORTS'
  | 'PROGRESS_PHOTOS'
  | 'CUSTOM_WORKOUTS'
  | 'DATA_EXPORT'
  | 'AI_ASSISTANT';

export type BillingCycle = 'MONTHLY' | 'YEARLY';
export type SubscriptionStatus = 'ACTIVE' | 'TRIALING' | 'EXPIRED' | 'CANCELED';
export type SubscriptionSource = 'FREE' | 'TRIAL' | 'PAYMENT' | 'ADMIN_GRANT';
export type PaymentStatus = 'CREATED' | 'PAID' | 'FAILED';

export interface PlanFeature {
  code: Feature;
  /** null = unlimited */
  limit: number | null;
}

export interface Plan {
  code: string;
  name: string;
  description: string | null;
  priceMonthlyMinor: number;
  priceYearlyMinor: number;
  currency: string;
  free: boolean;
  active: boolean;
  features: PlanFeature[];
}

export interface Subscription {
  planCode: string;
  planName: string;
  premium: boolean;
  status: SubscriptionStatus;
  source: SubscriptionSource;
  billingCycle: BillingCycle | null;
  currentPeriodStart: string;
  currentPeriodEnd: string | null;
  daysRemaining: number | null;
  features: PlanFeature[];
}

export interface BillingConfig {
  paymentsEnabled: boolean;
  trialDays: number;
  provider: string;
}

export interface CheckoutResponse {
  provider: string;
  keyId: string;
  orderId: string;
  amountMinor: number;
  currency: string;
  productName: string;
  description: string;
  prefillName: string | null;
  prefillEmail: string;
}

export interface Payment {
  id: number;
  planCode: string;
  planName: string;
  billingCycle: BillingCycle;
  amountMinor: number;
  currency: string;
  status: PaymentStatus;
  providerOrderId: string;
  providerPaymentId: string | null;
  paidAt: string | null;
  createdAt: string;
}
