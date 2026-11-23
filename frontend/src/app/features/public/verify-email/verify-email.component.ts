import { Component, Input, OnInit } from '@angular/core';
import { AuthService } from '../../../core/services/auth.service';
import { apiErrorMessage } from '../../../core/utils/api-error';

/** Landing page for the link in the verification email. Works whether or not the user is signed in. */
@Component({
  selector: 'ft-verify-email',
  standalone: false,
  template: `
    <section class="wrap">
      <mat-card appearance="outlined" class="card">
        @switch (state) {
          @case ('working') {
            <mat-spinner diameter="40" />
            <p>Verifying your email…</p>
          }
          @case ('done') {
            <mat-icon class="icon ok" aria-hidden="true">mark_email_read</mat-icon>
            <h1>Email verified</h1>
            <p class="ft-muted">Thanks! Your email address is confirmed.</p>
            <a mat-flat-button [routerLink]="auth.isAuthenticated() ? '/dashboard' : '/auth/login'">
              {{ auth.isAuthenticated() ? 'Continue to FitTrack' : 'Sign in' }}
            </a>
          }
          @case ('error') {
            <mat-icon class="icon bad" aria-hidden="true">link_off</mat-icon>
            <h1>Link not valid</h1>
            <p class="ft-muted">{{ message }}</p>
            <a mat-flat-button [routerLink]="auth.isAuthenticated() ? '/dashboard' : '/auth/login'">
              {{ auth.isAuthenticated() ? 'Back to FitTrack' : 'Sign in to request a new link' }}
            </a>
          }
        }
      </mat-card>
    </section>
  `,
  styles: `
    .wrap { display: grid; place-items: center; padding: 64px 16px; }
    .card { max-width: 440px; width: 100%; padding: 32px 24px; border-radius: 24px; display: flex; flex-direction: column; align-items: center; text-align: center; gap: 8px; }
    h1 { margin: 4px 0 0; font: var(--mat-sys-headline-small); font-weight: 700; }
    .icon { width: 56px; height: 56px; font-size: 56px; }
    .ok { color: var(--mat-sys-primary); }
    .bad { color: var(--mat-sys-error); }
  `,
})
export class VerifyEmailComponent implements OnInit {
  @Input() token?: string;
  state: 'working' | 'done' | 'error' = 'working';
  message = '';

  constructor(readonly auth: AuthService) {}

  ngOnInit(): void {
    if (!this.token) {
      this.state = 'error';
      this.message = 'This link is incomplete.';
      return;
    }
    this.auth.verifyEmail(this.token).subscribe({
      next: () => (this.state = 'done'),
      error: (err) => {
        this.state = 'error';
        this.message = apiErrorMessage(err, 'This verification link is invalid or has expired.');
      },
    });
  }
}
