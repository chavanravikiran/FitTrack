import { Component } from '@angular/core';

/** Split-screen frame for all signed-out pages: brand panel + form card. */
@Component({
  selector: 'ft-auth-layout',
  standalone: false,
  template: `
    <div class="auth">
      <section class="auth__brand" aria-hidden="true">
        <div class="auth__brand-inner">
          <div class="logo"><mat-icon>bolt</mat-icon> FitTrack</div>
          <h2>Your personal fitness coach, built for beginners.</h2>
          <ul>
            <li><mat-icon>check_circle</mat-icon> Workouts matched to your goal, schedule and equipment</li>
            <li><mat-icon>check_circle</mat-icon> Track steps, water, sleep, weight and meals in one place</li>
            <li><mat-icon>check_circle</mat-icon> Safe, gradual progress - no crash diets, no overtraining</li>
          </ul>
        </div>
      </section>
      <main class="auth__main">
        <div class="auth__card">
          <div class="auth__mobile-logo"><mat-icon>bolt</mat-icon> FitTrack</div>
          <router-outlet />
        </div>
      </main>
    </div>
  `,
  styleUrl: './auth-layout.component.scss',
})
export class AuthLayoutComponent {}
