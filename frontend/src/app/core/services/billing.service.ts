import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, map, shareReplay, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SKIP_ERROR_TOAST } from '../http/http-context';
import { ApiResponse, PageResponse } from '../models/api.models';
import { BillingConfig, BillingCycle, CheckoutResponse, Feature, Payment, Plan, Subscription } from '../models/billing.models';
import { AuthService } from './auth.service';

@Injectable({ providedIn: 'root' })
export class BillingService {
  private readonly api = environment.apiUrl;
  private readonly subscriptionSubject = new BehaviorSubject<Subscription | null>(null);
  private config$?: Observable<BillingConfig>;

  /** Current user's plan; refreshed after payments and on demand. */
  readonly subscription$ = this.subscriptionSubject.asObservable();

  constructor(
    private readonly http: HttpClient,
    private readonly auth: AuthService,
  ) {}

  plans(): Observable<Plan[]> {
    return this.http.get<ApiResponse<Plan[]>>(`${this.api}/plans`).pipe(map((r) => r.data));
  }

  config(): Observable<BillingConfig> {
    this.config$ ??= this.http
      .get<ApiResponse<BillingConfig>>(`${this.api}/billing/config`)
      .pipe(map((r) => r.data), shareReplay(1));
    return this.config$;
  }

  loadSubscription(): Observable<Subscription> {
    return this.http.get<ApiResponse<Subscription>>(`${this.api}/me/subscription`).pipe(
      map((r) => r.data),
      tap((s) => this.setSubscription(s)),
    );
  }

  checkout(planCode: string, billingCycle: BillingCycle): Observable<CheckoutResponse> {
    return this.http
      .post<ApiResponse<CheckoutResponse>>(`${this.api}/me/billing/checkout`, { planCode, billingCycle }, {
        context: new HttpContext().set(SKIP_ERROR_TOAST, true),
      })
      .pipe(map((r) => r.data));
  }

  verify(orderId: string, paymentId: string, signature: string): Observable<Subscription> {
    return this.http
      .post<ApiResponse<Subscription>>(`${this.api}/me/billing/verify`, { orderId, paymentId, signature }, {
        context: new HttpContext().set(SKIP_ERROR_TOAST, true),
      })
      .pipe(
        map((r) => r.data),
        tap((s) => this.setSubscription(s)),
      );
  }

  payments(page = 0, size = 20): Observable<PageResponse<Payment>> {
    return this.http
      .get<ApiResponse<PageResponse<Payment>>>(`${this.api}/me/billing/payments`, { params: { page, size } })
      .pipe(map((r) => r.data));
  }

  /** Client-side hint for showing/hiding UI. The server always enforces entitlements. */
  hasFeature(feature: Feature): boolean {
    return this.subscriptionSubject.value?.features.some((f) => f.code === feature) ?? false;
  }

  clear(): void {
    this.subscriptionSubject.next(null);
  }

  private setSubscription(s: Subscription): void {
    this.subscriptionSubject.next(s);
    this.auth.patchUser({ planCode: s.planCode, premium: s.premium });
  }
}
