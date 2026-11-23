import { Component, OnInit } from '@angular/core';
import { PageEvent } from '@angular/material/paginator';
import { Payment, PaymentStatus } from '../../../core/models/billing.models';
import { AdminService } from '../../../core/services/admin.service';

@Component({
  selector: 'ft-admin-payments',
  standalone: false,
  template: `
    <div class="filters">
      <mat-form-field appearance="outline" subscriptSizing="dynamic">
        <mat-label>Status</mat-label>
        <mat-select [(value)]="status" (selectionChange)="load(true)">
          <mat-option value="">All</mat-option>
          <mat-option value="PAID">Paid</mat-option>
          <mat-option value="CREATED">Not completed</mat-option>
          <mat-option value="FAILED">Failed</mat-option>
        </mat-select>
      </mat-form-field>
    </div>
    <mat-card appearance="outlined" class="table-card">
      @if (loading) { <mat-progress-bar mode="indeterminate" /> }
      <div class="table-wrap">
        <table mat-table [dataSource]="rows">
          <ng-container matColumnDef="date">
            <th mat-header-cell *matHeaderCellDef>Date</th>
            <td mat-cell *matCellDef="let p">{{ (p.paidAt ?? p.createdAt) | date: 'medium' }}</td>
          </ng-container>
          <ng-container matColumnDef="plan">
            <th mat-header-cell *matHeaderCellDef>Plan</th>
            <td mat-cell *matCellDef="let p">{{ p.planName }} · {{ p.billingCycle | titlecase }}</td>
          </ng-container>
          <ng-container matColumnDef="amount">
            <th mat-header-cell *matHeaderCellDef>Amount</th>
            <td mat-cell *matCellDef="let p">{{ p.amountMinor | money: p.currency }}</td>
          </ng-container>
          <ng-container matColumnDef="status">
            <th mat-header-cell *matHeaderCellDef>Status</th>
            <td mat-cell *matCellDef="let p"><span class="status" [class]="'status--' + p.status.toLowerCase()">{{ p.status | titlecase }}</span></td>
          </ng-container>
          <ng-container matColumnDef="order">
            <th mat-header-cell *matHeaderCellDef>Razorpay order / payment</th>
            <td mat-cell *matCellDef="let p" class="mono">{{ p.providerOrderId }}<br />{{ p.providerPaymentId ?? '—' }}</td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="columns"></tr>
          <tr mat-row *matRowDef="let row; columns: columns"></tr>
          <tr class="mat-row" *matNoDataRow><td class="mat-cell empty" [attr.colspan]="columns.length">No payments yet.</td></tr>
        </table>
      </div>
      <mat-paginator [length]="total" [pageIndex]="page" [pageSize]="size" [pageSizeOptions]="[20, 50, 100]" (page)="onPage($event)"
                     aria-label="Select page of payments" />
    </mat-card>
  `,
  styles: `
    .filters { margin-bottom: 16px; }
    .table-card { border-radius: 20px; overflow: hidden; }
    .table-wrap { overflow-x: auto; }
    table { width: 100%; min-width: 680px; background: transparent; }
    .mono { font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 12px; }
    .status { padding: 2px 8px; border-radius: 999px; font: var(--mat-sys-label-medium); }
    .status--paid { background: var(--mat-sys-primary-container); color: var(--mat-sys-on-primary-container); }
    .status--failed { background: var(--mat-sys-error-container); color: var(--mat-sys-on-error-container); }
    .status--created { background: var(--mat-sys-surface-container-highest); }
    .empty { padding: 24px; text-align: center; color: var(--mat-sys-on-surface-variant); }
  `,
})
export class AdminPaymentsComponent implements OnInit {
  rows: Payment[] = [];
  total = 0;
  page = 0;
  size = 20;
  status: PaymentStatus | '' = '';
  loading = false;
  readonly columns = ['date', 'plan', 'amount', 'status', 'order'];

  constructor(private readonly admin: AdminService) {}

  ngOnInit(): void {
    this.load();
  }

  load(reset = false): void {
    if (reset) this.page = 0;
    this.loading = true;
    this.admin.payments(this.status, this.page, this.size).subscribe({
      next: (res) => {
        this.rows = res.content;
        this.total = res.totalElements;
        this.loading = false;
      },
      error: () => (this.loading = false),
    });
  }

  onPage(e: PageEvent): void {
    this.page = e.pageIndex;
    this.size = e.pageSize;
    this.load();
  }
}
