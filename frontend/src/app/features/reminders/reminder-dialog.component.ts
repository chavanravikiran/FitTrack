import { Component, inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { finalize } from 'rxjs';
import { DayOfWeek } from '../../core/models/assessment.models';
import { Reminder, ReminderChannel, ReminderType } from '../../core/models/reminder.models';
import { ReminderService } from '../../core/services/reminder.service';
import { apiErrorMessage } from '../../core/utils/api-error';
import { DAY_OPTIONS } from '../../shared/constants/options';
import { REMINDER_TYPES } from '../../shared/constants/labels';

const ALL_DAYS: DayOfWeek[] = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];

@Component({
  selector: 'ft-reminder-dialog',
  standalone: false,
  template: `
    <h2 mat-dialog-title>{{ data.reminder ? 'Edit reminder' : 'New reminder' }}</h2>
    <mat-dialog-content>
      @if (error) { <p class="error" role="alert">{{ error }}</p> }
      <form [formGroup]="form" id="reminder-form" class="ft-form" (ngSubmit)="save()">
        <mat-form-field appearance="outline">
          <mat-label>Type</mat-label>
          <mat-select formControlName="type" (selectionChange)="onType($event.value)">
            @for (t of types; track t.value) { <mat-option [value]="t.value"><mat-icon>{{ t.icon }}</mat-icon> {{ t.label }}</mat-option> }
          </mat-select>
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Title</mat-label>
          <input matInput formControlName="title" maxlength="100" />
          <mat-error>{{ form.controls.title | formError: 'Title' }}</mat-error>
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Message (optional)</mat-label>
          <input matInput formControlName="message" maxlength="255" />
        </mat-form-field>
        <div class="ft-form-grid">
          <mat-form-field appearance="outline">
            <mat-label>{{ form.controls.repeat.value ? 'Start time' : 'Time' }}</mat-label>
            <input matInput type="time" formControlName="time" />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Send as</mat-label>
            <mat-select formControlName="channel">
              <mat-option value="IN_APP">App notification</mat-option>
              <mat-option value="EMAIL">Email + app</mat-option>
            </mat-select>
          </mat-form-field>
        </div>
        <mat-slide-toggle formControlName="repeat">Repeat during the day</mat-slide-toggle>
        @if (form.controls.repeat.value) {
          <div class="ft-form-grid repeat">
            <mat-form-field appearance="outline">
              <mat-label>Every</mat-label>
              <mat-select formControlName="intervalMinutes">
                @for (m of intervals; track m) { <mat-option [value]="m">{{ m < 60 ? m + ' min' : (m / 60) + ' h' }}</mat-option> }
              </mat-select>
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Until</mat-label>
              <input matInput type="time" formControlName="endTime" />
            </mat-form-field>
          </div>
        }
        <label class="label" id="days-label">Days</label>
        <mat-chip-listbox formControlName="days" [multiple]="true" aria-labelledby="days-label">
          @for (d of dayOptions; track d.value) { <mat-chip-option [value]="d.value">{{ d.label }}</mat-chip-option> }
        </mat-chip-listbox>
        @if (form.controls.days.invalid) { <mat-error class="small">Pick at least one day</mat-error> }
        <p class="ft-muted small">Times use your profile time zone.</p>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancel</button>
      <button mat-flat-button type="submit" form="reminder-form" [disabled]="saving">Save</button>
    </mat-dialog-actions>
  `,
  styles: `.error { color: var(--mat-sys-error); } .label { font: var(--mat-sys-title-small); margin-top: 12px; display: block; }
           .small { font: var(--mat-sys-body-small); } .repeat { margin-top: 12px; } mat-option mat-icon { vertical-align: middle; }`,
})
export class ReminderDialogComponent {
  private readonly fb = inject(FormBuilder);
  readonly data = inject<{ reminder?: Reminder; type?: ReminderType }>(MAT_DIALOG_DATA);
  readonly types = REMINDER_TYPES;
  readonly dayOptions = DAY_OPTIONS;
  readonly intervals = [30, 60, 90, 120, 180];

  private readonly initialType: ReminderType = this.data.reminder?.type ?? this.data.type ?? 'WATER';
  private readonly preset = REMINDER_TYPES.find((t) => t.value === this.initialType)!;

  readonly form = this.fb.group({
    type: this.fb.nonNullable.control<ReminderType>(this.initialType, Validators.required),
    title: this.fb.nonNullable.control(this.data.reminder?.title ?? this.preset.defaultTitle, [Validators.required, Validators.maxLength(100)]),
    message: this.fb.control<string | null>(this.data.reminder?.message ?? null),
    time: this.fb.nonNullable.control(this.data.reminder?.time ?? this.preset.defaultTime, Validators.required),
    channel: this.fb.nonNullable.control<ReminderChannel>(this.data.reminder?.channel === 'EMAIL' ? 'EMAIL' : 'IN_APP'),
    repeat: this.fb.nonNullable.control(!!this.data.reminder?.intervalMinutes || (!this.data.reminder && this.initialType === 'WATER')),
    intervalMinutes: this.fb.nonNullable.control(this.data.reminder?.intervalMinutes ?? 120),
    endTime: this.fb.nonNullable.control(this.data.reminder?.endTime ?? '20:00'),
    days: this.fb.nonNullable.control<DayOfWeek[]>(this.data.reminder?.days ?? ALL_DAYS,
      (c) => (c.value as DayOfWeek[]).length ? null : { required: true }),
  });
  saving = false;
  error: string | null = null;

  constructor(
    private readonly reminders: ReminderService,
    private readonly ref: MatDialogRef<ReminderDialogComponent, boolean>,
  ) {}

  onType(type: ReminderType): void {
    const p = REMINDER_TYPES.find((t) => t.value === type)!;
    if (!this.data.reminder) {
      this.form.patchValue({ title: p.defaultTitle, time: p.defaultTime, repeat: type === 'WATER' });
    }
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const req = {
      type: v.type, title: v.title.trim(), message: v.message?.trim() || null, time: v.time, days: v.days,
      intervalMinutes: v.repeat ? v.intervalMinutes : null, endTime: v.repeat ? v.endTime : null,
      channel: v.channel, enabled: this.data.reminder?.enabled ?? true,
    };
    this.saving = true;
    const call = this.data.reminder ? this.reminders.update(this.data.reminder.id, req) : this.reminders.create(req);
    call.pipe(finalize(() => (this.saving = false))).subscribe({
      next: () => this.ref.close(true),
      error: (err) => (this.error = apiErrorMessage(err)),
    });
  }
}
