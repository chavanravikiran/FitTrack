import { Component } from '@angular/core';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'ft-public-layout',
  standalone: false,
  template: `
    <header class="nav">
      <a class="logo" routerLink="/"><mat-icon aria-hidden="true">bolt</mat-icon> FitTrack</a>
      <nav class="nav__links" aria-label="Site">
        <a mat-button routerLink="/" fragment="features">Features</a>
        <a mat-button routerLink="/pricing">Pricing</a>
      </nav>
      <span class="spacer"></span>
      @if (auth.isAuthenticated()) {
        <a mat-flat-button routerLink="/dashboard">Open app</a>
      } @else {
        <a mat-button routerLink="/auth/login">Sign in</a>
        <a mat-flat-button routerLink="/auth/register">Get started</a>
      }
    </header>
    <main><router-outlet /></main>
    <footer class="footer">
      <div class="footer__inner">
        <div>
          <div class="logo"><mat-icon aria-hidden="true">bolt</mat-icon> FitTrack</div>
          <p class="ft-muted small">Educational fitness guidance, not medical advice.</p>
        </div>
        <nav class="footer__links" aria-label="Legal">
          <a routerLink="/pricing">Pricing</a>
          <a routerLink="/terms">Terms of Service</a>
          <a routerLink="/privacy">Privacy Policy</a>
        </nav>
      </div>
      <p class="ft-muted small copy">© {{ year }} FitTrack. All rights reserved.</p>
    </footer>
  `,
  styles: `
    :host { display: flex; flex-direction: column; min-height: 100vh; background: var(--mat-sys-surface); }
    main { flex: 1; }
    .nav { position: sticky; top: 0; z-index: 5; display: flex; align-items: center; gap: 4px; padding: 10px 16px;
           background: color-mix(in srgb, var(--mat-sys-surface) 88%, transparent); backdrop-filter: blur(8px);
           border-bottom: 1px solid var(--mat-sys-outline-variant); }
    .logo { display: inline-flex; align-items: center; gap: 6px; font: var(--mat-sys-title-large); font-weight: 700;
            color: var(--mat-sys-primary); text-decoration: none; margin-right: 12px; }
    .nav__links { display: none; }
    @media (min-width: 720px) { .nav__links { display: flex; } }
    .spacer { flex: 1; }
    .footer { border-top: 1px solid var(--mat-sys-outline-variant); background: var(--mat-sys-surface-container-low); padding: 32px 16px 20px; }
    .footer__inner { max-width: 1120px; margin: 0 auto; display: flex; flex-wrap: wrap; gap: 24px; justify-content: space-between; }
    .footer__links { display: flex; gap: 20px; flex-wrap: wrap; }
    .small { font: var(--mat-sys-body-small); }
    .copy { max-width: 1120px; margin: 16px auto 0; }
  `,
})
export class PublicLayoutComponent {
  readonly year = new Date().getFullYear();
  constructor(readonly auth: AuthService) {}
}
