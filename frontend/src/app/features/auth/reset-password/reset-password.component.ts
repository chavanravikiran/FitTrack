import { Component, Input, inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { apiErrorMessage } from '../../../core/utils/api-error';
import { matchFields, strongPassword } from '../../../shared/forms/validators';

@Component({
  selector: 'ft-reset-password',
  standalone: false,
  template: `
    <h1>Choose a new password</h1>
    <p class="lead">For your security, this signs you out of FitTrack on all other devices.</p>

    @if (!token) {
      <div class="alert alert--error" role="alert"><mat-icon>link_off</mat-icon> This reset link is incomplete. Please request a new one.</div>
      <p class="alt"><a routerLink="/auth/forgot-password">Request a new link</a></p>
    } @else {
      @if (error) {
        <div class="alert alert--error" role="alert"><mat-icon>error</mat-icon> {{ error }}</div>
      }
      <form [formGroup]="form" (ngSubmit)="submit()" class="ft-form" novalidate>
        <mat-form-field appearance="outline">
          <mat-label>New password</mat-label>
          <input matInput [type]="hide ? 'password' : 'text'" formControlName="newPassword" autocomplete="new-password" required />
          <button mat-icon-button matSuffix type="button" (click)="hide = !hide" [attr.aria-label]="hide ? 'Show password' : 'Hide password'">
            <mat-icon>{{ hide ? 'visibility' : 'visibility_off' }}</mat-icon>
          </button>
          <mat-hint>8+ characters with upper-case, lower-case and a number</mat-hint>
          <mat-error>{{ form.controls.newPassword | formError: 'Password' }}</mat-error>
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Confirm new password</mat-label>
          <input matInput [type]="hide ? 'password' : 'text'" formControlName="confirmPassword" autocomplete="new-password" required />
          <mat-error>{{ form.controls.confirmPassword | formError: 'Confirmation' }}</mat-error>
        </mat-form-field>
        <button mat-flat-button class="submit" type="submit" [disabled]="submitting">
          @if (submitting) { <mat-spinner diameter="20" /> } @else { Update password }
        </button>
      </form>
    }
  `,
  styleUrl: '../auth-forms.scss',
})
export class ResetPasswordComponent {
  private readonly fb = inject(FormBuilder);
  @Input() token?: string;

  readonly form = this.fb.nonNullable.group(
    {
      newPassword: ['', [Validators.required, strongPassword]],
      confirmPassword: ['', [Validators.required]],
    },
    { validators: matchFields('newPassword', 'confirmPassword') },
  );
  submitting = false;
  hide = true;
  error: string | null = null;

  constructor(
    private readonly auth: AuthService,
    private readonly router: Router,
  ) {}

  submit(): void {
    if (this.form.invalid || !this.token) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting = true;
    this.error = null;
    this.auth
      .resetPassword({ token: this.token, newPassword: this.form.getRawValue().newPassword })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: () => this.router.navigate(['/auth/login'], { queryParams: { reset: 1 } }),
        error: (err) => (this.error = apiErrorMessage(err)),
      });
  }
}
