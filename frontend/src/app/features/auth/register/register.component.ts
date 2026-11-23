import { Component, inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { apiErrorMessage, applyServerErrors } from '../../../core/utils/api-error';
import { browserTimeZone, matchFields, strongPassword } from '../../../shared/forms/validators';

@Component({
  selector: 'ft-register',
  standalone: false,
  templateUrl: './register.component.html',
  styleUrl: '../auth-forms.scss',
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);
  readonly form = this.fb.nonNullable.group(
    {
      fullName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      email: ['', [Validators.required, Validators.email, Validators.maxLength(255)]],
      password: ['', [Validators.required, strongPassword]],
      confirmPassword: ['', [Validators.required]],
      acceptTerms: [false, [Validators.requiredTrue]],
    },
    { validators: matchFields('password', 'confirmPassword') },
  );

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
    const { fullName, email, password, acceptTerms } = this.form.getRawValue();
    this.auth
      .register({ fullName: fullName.trim(), email: email.trim(), password, timeZone: browserTimeZone(), acceptTerms })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: () => this.router.navigateByUrl('/onboarding'),
        error: (err) => {
          const unmapped = applyServerErrors(this.form, err);
          this.error = unmapped.length || !this.form.invalid ? apiErrorMessage(err) : null;
        },
      });
  }
}
