import { Component, inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { apiErrorMessage } from '../../../core/utils/api-error';

@Component({
  selector: 'ft-forgot-password',
  standalone: false,
  template: `
    <h1>Reset your password</h1>
    <p class="lead">Enter the email you signed up with and we'll send you a link to choose a new password.</p>

    @if (sentMessage) {
      <div class="alert alert--success" role="status"><mat-icon>mark_email_read</mat-icon> {{ sentMessage }}</div>
      <p class="alt"><a routerLink="/auth/login">Back to sign in</a></p>
    } @else {
      @if (error) {
        <div class="alert alert--error" role="alert"><mat-icon>error</mat-icon> {{ error }}</div>
      }
      <form [formGroup]="form" (ngSubmit)="submit()" class="ft-form" novalidate>
        <mat-form-field appearance="outline">
          <mat-label>Email</mat-label>
          <input matInput type="email" formControlName="email" autocomplete="email" inputmode="email" required />
          <mat-icon matPrefix>mail</mat-icon>
          <mat-error>{{ form.controls.email | formError: 'Email' }}</mat-error>
        </mat-form-field>
        <button mat-flat-button class="submit" type="submit" [disabled]="submitting">
          @if (submitting) { <mat-spinner diameter="20" /> } @else { Send reset link }
        </button>
      </form>
      <p class="alt"><a routerLink="/auth/login">Back to sign in</a></p>
    }
  `,
  styleUrl: '../auth-forms.scss',
})
export class ForgotPasswordComponent {
  private readonly fb = inject(FormBuilder);
  readonly form = this.fb.nonNullable.group({ email: ['', [Validators.required, Validators.email]] });
  submitting = false;
  sentMessage: string | null = null;
  error: string | null = null;

  constructor(
    private readonly auth: AuthService,
  ) {}

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting = true;
    this.error = null;
    this.auth
      .forgotPassword(this.form.getRawValue().email.trim())
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: (msg) => (this.sentMessage = msg || 'If an account exists for that email, a reset link has been sent.'),
        error: (err) => (this.error = apiErrorMessage(err)),
      });
  }
}
