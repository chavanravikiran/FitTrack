import { Component, inject } from '@angular/core';
import { FormControl, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { finalize } from 'rxjs';
import { NotifyService } from '../../../core/services/notify.service';
import { WorkoutService } from '../../../core/services/workout.service';
import { apiErrorMessage } from '../../../core/utils/api-error';
import { toIsoDate } from '../../../shared/forms/validators';

export interface RescheduleDialogData {
  date: string;
  planDayId: number;
  title: string;
}

@Component({
  selector: 'ft-reschedule-dialog',
  standalone: false,
  template: `
    <h2 mat-dialog-title>Move workout</h2>
    <mat-dialog-content>
      <p class="ft-muted">Move <strong>{{ data.title }}</strong> to another day within the next two weeks.</p>
      @if (error) { <p class="error" role="alert">{{ error }}</p> }
      <mat-form-field appearance="outline" class="ft-full-width">
        <mat-label>New date</mat-label>
        <input matInput [matDatepicker]="picker" [formControl]="date" [min]="min" [max]="max" />
        <mat-datepicker-toggle matIconSuffix [for]="picker" />
        <mat-datepicker #picker />
      </mat-form-field>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancel</button>
      <button mat-flat-button (click)="save()" [disabled]="date.invalid || saving">Move</button>
    </mat-dialog-actions>
  `,
  styles: `.error { color: var(--mat-sys-error); }`,
})
export class RescheduleDialogComponent {
  readonly data = inject<RescheduleDialogData>(MAT_DIALOG_DATA);
  readonly min = new Date();
  readonly max = (() => { const d = new Date(); d.setDate(d.getDate() + 14); return d; })();
  readonly date = new FormControl<Date | null>(null, Validators.required);
  saving = false;
  error: string | null = null;

  constructor(
    private readonly workouts: WorkoutService,
    private readonly notify: NotifyService,
    private readonly ref: MatDialogRef<RescheduleDialogComponent, boolean>,
  ) {}

  save(): void {
    const newDate = toIsoDate(this.date.value);
    if (!newDate) return;
    this.saving = true;
    this.workouts.reschedule(this.data.date, this.data.planDayId, newDate)
      .pipe(finalize(() => (this.saving = false)))
      .subscribe({
        next: () => {
          this.notify.success('Workout moved');
          this.ref.close(true);
        },
        error: (err) => (this.error = apiErrorMessage(err)),
      });
  }
}
