import { DOCUMENT } from '@angular/common';
import { Inject, Injectable, NgZone } from '@angular/core';
import { Observable } from 'rxjs';
import { CheckoutResponse } from '../models/billing.models';

const CHECKOUT_SRC = 'https://checkout.razorpay.com/v1/checkout.js';

export interface RazorpaySuccess {
  razorpay_payment_id: string;
  razorpay_order_id: string;
  razorpay_signature: string;
}

interface RazorpayInstance {
  open(): void;
  on(event: string, handler: (resp: { error?: { description?: string } }) => void): void;
}

type RazorpayCtor = new (options: Record<string, unknown>) => RazorpayInstance;

/**
 * Loads Razorpay Standard Checkout on demand and opens it for a server-created order.
 * Emits the signed result on success; errors on failure; completes without a value if dismissed.
 */
@Injectable({ providedIn: 'root' })
export class RazorpayService {
  private loading?: Promise<void>;

  constructor(
    @Inject(DOCUMENT) private readonly document: Document,
    private readonly zone: NgZone,
  ) {}

  pay(order: CheckoutResponse, themeColor: string): Observable<RazorpaySuccess> {
    return new Observable<RazorpaySuccess>((subscriber) => {
      this.loadScript()
        .then(() => {
          const Razorpay = (this.document.defaultView as unknown as { Razorpay?: RazorpayCtor }).Razorpay;
          if (!Razorpay) throw new Error('Razorpay Checkout failed to load');
          const rzp = new Razorpay({
            key: order.keyId,
            order_id: order.orderId,
            amount: order.amountMinor,
            currency: order.currency,
            name: order.productName,
            description: order.description,
            prefill: { name: order.prefillName ?? '', email: order.prefillEmail },
            theme: { color: themeColor },
            handler: (resp: RazorpaySuccess) => this.zone.run(() => { subscriber.next(resp); subscriber.complete(); }),
            modal: { ondismiss: () => this.zone.run(() => subscriber.complete()) },
          });
          rzp.on('payment.failed', (resp) =>
            this.zone.run(() => subscriber.error(new Error(resp.error?.description ?? 'Payment failed'))),
          );
          rzp.open();
        })
        .catch((e) => this.zone.run(() => subscriber.error(e)));
    });
  }

  private loadScript(): Promise<void> {
    this.loading ??= new Promise<void>((resolve, reject) => {
      const script = this.document.createElement('script');
      script.src = CHECKOUT_SRC;
      script.async = true;
      script.onload = () => resolve();
      script.onerror = () => {
        this.loading = undefined;
        reject(new Error('Could not load the payment window. Check your connection and try again.'));
      };
      this.document.body.appendChild(script);
    });
    return this.loading;
  }
}
