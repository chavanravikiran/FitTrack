import { Component, OnInit, inject } from '@angular/core';
import { MAT_DIALOG_DATA } from '@angular/material/dialog';
import { AdminUserDetail } from '../../../core/models/admin.models';
import { AdminService } from '../../../core/services/admin.service';

@Component({
  selector: 'ft-user-detail-dialog',
  standalone: false,
  template: `
    <h2 mat-dialog-title>User details</h2>
    <mat-dialog-content>
      @if (detail; as d) {
        <dl class="facts">
          <div><dt>Name</dt><dd>{{ d.user.fullName || '—' }}</dd></div>
          <div><dt>Email</dt><dd>{{ d.user.email }} {{ d.user.emailVerified ? '(verified)' : '(not verified)' }}</dd></div>
          <div><dt>Roles</dt><dd>{{ d.roles.join(', ') }}</dd></div>
          <div><dt>Status</dt><dd>{{ d.user.enabled ? 'Enabled' : 'Disabled' }}</dd></div>
          <div><dt>Joined</dt><dd>{{ d.user.createdAt | date: 'medium' }}</dd></div>
          <div><dt>Last sign-in</dt><dd>{{ d.user.lastLoginAt ? (d.user.lastLoginAt | date: 'medium') : '—' }}</dd></div>
          <div><dt>Terms accepted</dt><dd>{{ d.termsAcceptedAt ? (d.termsAcceptedAt | date: 'mediumDate') + ' (v' + d.termsVersion + ')' : '—' }}</dd></div>
          <div><dt>Onboarding</dt><dd>{{ d.user.onboardingCompleted ? 'Completed' : 'Not completed' }}</dd></div>
        </dl>

        <h3>Subscription history</h3>
        <ul class="history">
          @for (s of d.subscriptions; track s.id) {
            <li [class.live]="s.status === 'ACTIVE' || s.status === 'TRIALING'">
              <strong>{{ s.planCode }}</strong> · {{ s.status | titlecase }} · {{ s.source | titlecase }}
              @if (s.billingCycle) { · {{ s.billingCycle | titlecase }} }
              <div class="ft-muted small">
                {{ s.currentPeriodStart | date: 'mediumDate' }} → {{ s.currentPeriodEnd ? (s.currentPeriodEnd | date: 'mediumDate') : 'no end' }}
                @if (s.notes) { · {{ s.notes }} }
              </div>
            </li>
          }
        </ul>

        <h3>Payments</h3>
        @if (d.payments.length) {
          <ul class="history">
            @for (p of d.payments; track p.id) {
              <li>
                <strong>{{ p.amountMinor | money: p.currency }}</strong> · {{ p.planName }} {{ p.billingCycle | titlecase }} · {{ p.status | titlecase }}
                <div class="ft-muted small">{{ (p.paidAt ?? p.createdAt) | date: 'medium' }} · {{ p.providerPaymentId ?? p.providerOrderId }}</div>
              </li>
            }
          </ul>
        } @else {
          <p class="ft-muted">No payments.</p>
        }
      } @else {
        <mat-progress-bar mode="indeterminate" />
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end"><button mat-button mat-dialog-close>Close</button></mat-dialog-actions>
  `,
  styles: `
    .facts { display: grid; grid-template-columns: 1fr; margin: 0; }
    @media (min-width: 600px) { .facts { grid-template-columns: 1fr 1fr; column-gap: 24px; } }
    .facts div { display: flex; flex-direction: column; padding: 6px 0; border-bottom: 1px solid var(--mat-sys-outline-variant); }
    dt { font: var(--mat-sys-label-medium); color: var(--mat-sys-on-surface-variant); }
    dd { margin: 0; }
    h3 { font: var(--mat-sys-title-small); margin: 20px 0 8px; }
    .history { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 8px; }
    .history li { padding: 8px 12px; border-radius: 12px; background: var(--mat-sys-surface-container-low); }
    .history li.live { outline: 2px solid var(--mat-sys-primary); }
    .small { font: var(--mat-sys-body-small); }
  `,
})
export class UserDetailDialogComponent implements OnInit {
  private readonly userId = inject<number>(MAT_DIALOG_DATA);
  detail: AdminUserDetail | null = null;

  constructor(private readonly admin: AdminService) {}

  ngOnInit(): void {
    this.admin.user(this.userId).subscribe((d) => (this.detail = d));
  }
}
