import { Component, inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { finalize } from 'rxjs';
import { AdminUserRow } from '../../../core/models/admin.models';
import { Plan } from '../../../core/models/billing.models';
import { AdminService } from '../../../core/services/admin.service';
import { apiErrorMessage } from '../../../core/utils/api-error';

export interface GrantDialogData {
  user: AdminUserRow;
  plans: Plan[];
}

@Component({
  selector: 'ft-grant-dialog',
  standalone: false,
  template: `
    <h2 mat-dialog-title>Grant plan</h2>
    <mat-dialog-content>
      <p class="ft-muted">For <strong>{{ data.user.email }}</strong>. If they already have this plan, the time is added to it.</p>
      @if (error) { <p class="error" role="alert">{{ error }}</p> }
      <form [formGroup]="form" id="grant-form" class="ft-form" (ngSubmit)="save()">
        <mat-form-field appearance="outline">
          <mat-label>Plan</mat-label>
          <mat-select formControlName="planCode">
            @for (p of data.plans; track p.code) { <mat-option [value]="p.code">{{ p.name }}</mat-option> }
          </mat-select>
        </mat-form-field>
        <mat-chip-listbox aria-label="Quick durations" class="quick">
          @for (d of quick; track d.days) {
            <mat-chip-option [selected]="form.controls.days.value === d.days" (click)="form.controls.days.setValue(d.days)">{{ d.label }}</mat-chip-option>
          }
        </mat-chip-listbox>
        <mat-form-field appearance="outline">
          <mat-label>Days</mat-label>
          <input matInput type="number" formControlName="days" min="1" max="3650" />
          <mat-error>{{ form.controls.days | formError: 'Days' }}</mat-error>
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Note (optional)</mat-label>
          <input matInput formControlName="note" maxlength="255" placeholder="e.g. Support goodwill, promo winner" />
        </mat-form-field>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close type="button">Cancel</button>
      <button mat-flat-button type="submit" form="grant-form" [disabled]="saving">Grant</button>
    </mat-dialog-actions>
  `,
  styles: `.quick { margin-bottom: 12px; } .error { color: var(--mat-sys-error); }`,
})
export class GrantDialogComponent {
  private readonly fb = inject(FormBuilder);
  readonly data = inject<GrantDialogData>(MAT_DIALOG_DATA);
  readonly quick = [
    { label: '7 days', days: 7 },
    { label: '1 month', days: 30 },
    { label: '3 months', days: 90 },
    { label: '1 year', days: 365 },
  ];
  readonly form = this.fb.nonNullable.group({
    planCode: [this.data.plans[0]?.code ?? '', Validators.required],
    days: [30, [Validators.required, Validators.min(1), Validators.max(3650)]],
    note: [''],
  });
  saving = false;
  error: string | null = null;

  constructor(
    private readonly admin: AdminService,
    private readonly ref: MatDialogRef<GrantDialogComponent, AdminUserRow>,
  ) {}

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    this.saving = true;
    this.admin
      .grant(this.data.user.id, v.planCode, Number(v.days), v.note.trim() || null)
      .pipe(finalize(() => (this.saving = false)))
      .subscribe({ next: (u) => this.ref.close(u), error: (e) => (this.error = apiErrorMessage(e)) });
  }
}
