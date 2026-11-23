import { BillingCycle, Payment, PlanFeature, SubscriptionSource, SubscriptionStatus } from './billing.models';

export interface DayCount {
  date: string;
  count: number;
}

export interface AdminStats {
  totalUsers: number;
  newUsersLast30Days: number;
  activeUsersLast30Days: number;
  premiumUsers: number;
  trialingUsers: number;
  revenueLast30DaysMinor: number;
  mrrMinor: number;
  currency: string;
  paymentsEnabled: boolean;
  signupsLast30Days: DayCount[];
}

export interface AdminUserRow {
  id: number;
  email: string;
  fullName: string | null;
  enabled: boolean;
  emailVerified: boolean;
  onboardingCompleted: boolean;
  planCode: string | null;
  subscriptionStatus: SubscriptionStatus | null;
  periodEnd: string | null;
  createdAt: string;
  lastLoginAt: string | null;
}

export interface SubscriptionHistory {
  id: number;
  planCode: string;
  status: SubscriptionStatus;
  source: SubscriptionSource;
  billingCycle: BillingCycle | null;
  currentPeriodStart: string;
  currentPeriodEnd: string | null;
  endedAt: string | null;
  notes: string | null;
}

export interface AdminUserDetail {
  user: AdminUserRow;
  roles: string[];
  termsVersion: string | null;
  termsAcceptedAt: string | null;
  subscriptions: SubscriptionHistory[];
  payments: Payment[];
}

export interface PlanUpdateRequest {
  name: string;
  description: string | null;
  priceMonthlyMinor: number;
  priceYearlyMinor: number;
  active: boolean;
  features: PlanFeature[];
}

export type UserStatusFilter = 'ANY' | 'ENABLED' | 'DISABLED';
