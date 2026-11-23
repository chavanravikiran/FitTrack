import { Component, Input, inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { apiErrorMessage } from '../../../core/utils/api-error';

@Component({
  selector: 'ft-login',
  standalone: false,
  templateUrl: './login.component.html',
  styleUrl: '../auth-forms.scss',
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  /** Bound from query params (router withComponentInputBinding). */
  @Input() returnUrl?: string;
  @Input() expired?: string;
  @Input() reset?: string;

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  submitting = false;
  hidePassword = true;
  error: string | null = null;

  constructor(
    private readonly auth: AuthService,
    private readonly router: Router,
  ) {}

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting = true;
    this.error = null;
    this.auth
      .login(this.form.getRawValue())
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: (user) => {
          const isAdmin = user.roles.includes('ROLE_ADMIN');
          const target = isAdmin && !this.returnUrl ? '/admin'
            : !user.onboardingCompleted && !isAdmin ? '/onboarding'
            : this.safeReturnUrl();
          this.router.navigateByUrl(target);
        },
        error: (err) => (this.error = apiErrorMessage(err, 'Sign-in failed. Please try again.')),
      });
  }

  /** Only allow in-app relative paths to avoid open redirects. */
  private safeReturnUrl(): string {
    const url = this.returnUrl;
    return url && url.startsWith('/') && !url.startsWith('//') && !url.startsWith('/auth') ? url : '/dashboard';
  }
}
