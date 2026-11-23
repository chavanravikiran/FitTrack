import { Component, inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { finalize } from 'rxjs';
import { Goal, GoalType } from '../../../core/models/goal.models';
import { GoalService } from '../../../core/services/goal.service';
import { apiErrorMessage } from '../../../core/utils/api-error';
import { GOAL_TYPE_OPTIONS } from '../../../shared/constants/options';
import { toIsoDate, fromIsoDate } from '../../../shared/forms/validators';

export interface GoalDialogData {
  goal?: Goal;
}

@Component({
  selector: 'ft-goal-dialog',
  standalone: false,
  template: `
    <h2 mat-dialog-title>{{ data.goal ? 'Edit goal' : 'New goal' }}</h2>
    <mat-dialog-content>
      @if (error) {
        <div class="alert" role="alert"><mat-icon>error</mat-icon> {{ error }}</div>
      }
      <form [formGroup]="form" class="ft-form" id="goal-form" (ngSubmit)="save()" novalidate>
        <mat-form-field appearance="outline">
          <mat-label>Goal type</mat-label>
          <mat-select formControlName="goalType">
            @for (o of types; track o.value) {
              <mat-option [value]="o.value"><mat-icon>{{ o.icon }}</mat-icon> {{ o.label }}</mat-option>
            }
          </mat-select>
          @if (!data.goal) {
            <mat-hint>Creating a goal replaces any active goal of the same type</mat-hint>
          }
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Target</mat-label>
          <input matInput type="number" formControlName="targetValue" [min]="selected.min" [max]="selected.max" [step]="selected.step" inputmode="decimal" />
          <span matTextSuffix>{{ selected.unit }}</span>
          <mat-hint>Between {{ selected.min }} and {{ selected.max }} {{ selected.unit }}</mat-hint>
          <mat-error>{{ form.controls.targetValue | formError: 'Target' }}</mat-error>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Title (optional)</mat-label>
          <input matInput formControlName="title" maxlength="120" />
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Target date (optional)</mat-label>
          <input matInput [matDatepicker]="picker" formControlName="targetDate" [min]="tomorrow" />
          <mat-datepicker-toggle matIconSuffix [for]="picker" />
          <mat-datepicker #picker />
          <mat-error>{{ form.controls.targetDate | formError: 'Date' }}</mat-error>
        </mat-form-field>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close type="button">Cancel</button>
      <button mat-flat-button type="submit" form="goal-form" [disabled]="saving">
        @if (saving) { <mat-spinner diameter="20" /> } @else { Save }
      </button>
    </mat-dialog-actions>
  `,
  styles: `.alert { display: flex; gap: 8px; padding: 12px; border-radius: 12px; margin-bottom: 12px; background: var(--mat-sys-error-container); color: var(--mat-sys-on-error-container); }
           mat-option mat-icon { vertical-align: middle; }`,
})
export class GoalDialogComponent {
  private readonly fb = inject(FormBuilder);
  readonly data = inject<GoalDialogData>(MAT_DIALOG_DATA);
  readonly types = GOAL_TYPE_OPTIONS;
  readonly tomorrow = (() => { const d = new Date(); d.setDate(d.getDate() + 1); return d; })();

  readonly form = this.fb.group({
    goalType: this.fb.nonNullable.control<GoalType>(this.data.goal?.goalType ?? 'DAILY_STEPS', Validators.required),
    targetValue: this.fb.control<number | null>(this.data.goal?.targetValue ?? null, Validators.required),
    title: this.fb.control<string | null>(this.data.goal?.title ?? null, Validators.maxLength(120)),
    targetDate: this.fb.control<Date | null>(fromIsoDate(this.data.goal?.targetDate ?? null)),
  });

  saving = false;
  error: string | null = null;

  constructor(
    private readonly goals: GoalService,
    private readonly ref: MatDialogRef<GoalDialogComponent, Goal>,
  ) {
    if (this.data.goal) {
      this.form.controls.goalType.disable(); // type is fixed once a goal exists
    }
    this.applyRange();
    this.form.controls.goalType.valueChanges.subscribe(() => this.applyRange());
  }

  get selected() {
    return this.types.find((t) => t.value === this.form.controls.goalType.value)!;
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const body = {
      title: v.title?.trim() || null,
      targetValue: Number(v.targetValue),
      targetDate: toIsoDate(v.targetDate),
    };
    this.saving = true;
    this.error = null;
    const call = this.data.goal
      ? this.goals.update(this.data.goal.id, body)
      : this.goals.create({ goalType: v.goalType, ...body });
    call.pipe(finalize(() => (this.saving = false))).subscribe({
      next: (goal) => this.ref.close(goal),
      error: (err) => (this.error = apiErrorMessage(err)),
    });
  }

  private applyRange(): void {
    const t = this.selected;
    const ctrl = this.form.controls.targetValue;
    ctrl.setValidators([Validators.required, Validators.min(t.min), Validators.max(t.max)]);
    ctrl.updateValueAndValidity();
  }
}
