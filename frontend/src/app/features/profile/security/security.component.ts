import { Component, inject } from '@angular/core';
import { FormBuilder, FormGroupDirective, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { NotifyService } from '../../../core/services/notify.service';
import { apiErrorMessage } from '../../../core/utils/api-error';
import { matchFields, strongPassword } from '../../../shared/forms/validators';

@Component({
  selector: 'ft-security',
  standalone: false,
  template: `
    <div class="layout">
      <mat-card appearance="outlined" class="ft-card">
        <h2 class="ft-section-title">Change password</h2>
        <p class="ft-muted sub">Changing your password signs you out on all other devices.</p>
        @if (error) {
          <div class="alert" role="alert"><mat-icon>error</mat-icon> {{ error }}</div>
        }
        <form [formGroup]="form" (ngSubmit)="submit(formDir)" #formDir="ngForm" class="ft-form" novalidate>
          <mat-form-field appearance="outline">
            <mat-label>Current password</mat-label>
            <input matInput type="password" formControlName="currentPassword" autocomplete="current-password" />
            <mat-error>{{ form.controls.currentPassword | formError: 'Current password' }}</mat-error>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>New password</mat-label>
            <input matInput type="password" formControlName="newPassword" autocomplete="new-password" />
            <mat-hint>8+ characters with upper-case, lower-case and a number</mat-hint>
            <mat-error>{{ form.controls.newPassword | formError: 'New password' }}</mat-error>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Confirm new password</mat-label>
            <input matInput type="password" formControlName="confirmPassword" autocomplete="new-password" />
            <mat-error>{{ form.controls.confirmPassword | formError: 'Confirmation' }}</mat-error>
          </mat-form-field>
          <div class="actions">
            <button mat-flat-button type="submit" [disabled]="saving">
              @if (saving) { <mat-spinner diameter="20" /> } @else { Update password }
            </button>
          </div>
        </form>
      </mat-card>

      <mat-card appearance="outlined" class="ft-card">
        <h2 class="ft-section-title">Session</h2>
        <p class="ft-muted">You are signed in as <strong>{{ auth.currentUser?.email }}</strong>.</p>
        <button mat-stroked-button type="button" (click)="auth.logout()"><mat-icon>logout</mat-icon> Sign out</button>
      </mat-card>

      <mat-card appearance="outlined" class="ft-card danger">
        <h2 class="ft-section-title">Delete account</h2>
        <p class="ft-muted">Permanently deletes your account, profile, fitness data, goals and uploaded photos. This can't be undone.
          Any remaining paid time is forfeited.</p>
        @if (deleteError) {
          <div class="alert" role="alert"><mat-icon>error</mat-icon> {{ deleteError }}</div>
        }
        <form [formGroup]="deleteForm" (ngSubmit)="deleteAccount()" class="ft-form" novalidate>
          <mat-form-field appearance="outline">
            <mat-label>Your password</mat-label>
            <input matInput type="password" formControlName="password" autocomplete="current-password" />
            <mat-error>{{ deleteForm.controls.password | formError: 'Password' }}</mat-error>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Type DELETE to confirm</mat-label>
            <input matInput formControlName="confirmation" autocomplete="off" />
            <mat-error>Type DELETE in capital letters</mat-error>
          </mat-form-field>
          <div class="actions">
            <button mat-flat-button class="danger-btn" type="submit" [disabled]="deleting || deleteForm.invalid">
              @if (deleting) { <mat-spinner diameter="20" /> } @else { Delete my account }
            </button>
          </div>
        </form>
      </mat-card>
    </div>
  `,
  styles: `
    .layout { display: grid; grid-template-columns: 1fr; gap: 16px; max-width: 960px; }
    @media (min-width: 960px) { .layout { grid-template-columns: 3fr 2fr; align-items: start; } }
    .sub { margin: -6px 0 16px; font: var(--mat-sys-body-small); }
    .actions { display: flex; justify-content: flex-end; margin-top: 12px; }
    .alert { display: flex; gap: 8px; padding: 12px; border-radius: 12px; margin-bottom: 12px; background: var(--mat-sys-error-container); color: var(--mat-sys-on-error-container); }
    .danger { border-color: var(--mat-sys-error); }
    .danger .ft-section-title { color: var(--mat-sys-error); }
    .danger-btn { --mat-button-filled-container-color: var(--mat-sys-error); --mat-button-filled-label-text-color: var(--mat-sys-on-error); }
  `,
})
export class SecurityComponent {
  private readonly fb = inject(FormBuilder);
  readonly form = this.fb.nonNullable.group(
    {
      currentPassword: ['', Validators.required],
      newPassword: ['', [Validators.required, strongPassword]],
      confirmPassword: ['', Validators.required],
    },
    { validators: matchFields('newPassword', 'confirmPassword') },
  );
  saving = false;
  error: string | null = null;

  readonly deleteForm = this.fb.nonNullable.group({
    password: ['', Validators.required],
    confirmation: ['', [Validators.required, Validators.pattern(/^DELETE$/)]],
  });
  deleting = false;
  deleteError: string | null = null;

  constructor(
    readonly auth: AuthService,
    private readonly notify: NotifyService,
    private readonly router: Router,
  ) {}

  deleteAccount(): void {
    if (this.deleteForm.invalid) {
      this.deleteForm.markAllAsTouched();
      return;
    }
    if (!confirm('Last check: permanently delete your FitTrack account and all your data?')) return;
    this.deleting = true;
    this.deleteError = null;
    this.auth
      .deleteAccount(this.deleteForm.getRawValue().password)
      .pipe(finalize(() => (this.deleting = false)))
      .subscribe({
        next: (msg) => {
          this.notify.success(msg || 'Your account has been deleted.');
          this.router.navigateByUrl('/');
        },
        error: (err) => (this.deleteError = apiErrorMessage(err)),
      });
  }

  submit(dir: FormGroupDirective): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { currentPassword, newPassword } = this.form.getRawValue();
    this.saving = true;
    this.error = null;
    this.auth
      .changePassword({ currentPassword, newPassword })
      .pipe(finalize(() => (this.saving = false)))
      .subscribe({
        next: () => {
          dir.resetForm();
          this.notify.success('Password changed. Other devices have been signed out.');
        },
        error: (err) => (this.error = apiErrorMessage(err)),
      });
  }
}
