import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { adminGuard, authGuard, guestGuard, needsOnboardingGuard, onboardedGuard } from './core/guards/auth.guards';
import { ShellComponent } from './layout/shell/shell.component';
import { ComingSoonComponent } from './shared/components/coming-soon.component';

const routes: Routes = [
  {
    path: 'auth',
    canMatch: [guestGuard],
    loadChildren: () => import('./features/auth/auth.module').then((m) => m.AuthModule),
  },
  {
    path: 'onboarding',
    canActivate: [needsOnboardingGuard],
    loadChildren: () => import('./features/onboarding/onboarding.module').then((m) => m.OnboardingModule),
  },
  // Public site: landing (signed-out only), pricing, legal pages, email verification.
  // Paths not found here fall through to the app shell below.
  {
    path: '',
    loadChildren: () => import('./features/public/public.module').then((m) => m.PublicModule),
  },
  {
    path: '',
    component: ShellComponent,
    canActivate: [authGuard, onboardedGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'dashboard',
        title: 'Dashboard · FitTrack',
        loadChildren: () => import('./features/dashboard/dashboard.module').then((m) => m.DashboardModule),
      },
      {
        path: 'profile',
        title: 'My Profile · FitTrack',
        loadChildren: () => import('./features/profile/profile.module').then((m) => m.ProfileModule),
      },
      {
        path: 'billing',
        title: 'Subscription · FitTrack',
        loadChildren: () => import('./features/billing/billing.module').then((m) => m.BillingModule),
      },
      {
        path: 'admin',
        canActivate: [adminGuard],
        loadChildren: () => import('./features/admin/admin.module').then((m) => m.AdminModule),
      },
      {
        path: 'workout',
        title: 'My Workout · FitTrack',
        loadChildren: () => import('./features/workout/workout.module').then((m) => m.WorkoutModule),
      },
      {
        path: 'activity',
        title: 'Activity · FitTrack',
        loadChildren: () => import('./features/activity/activity.module').then((m) => m.ActivityModule),
      },
      {
        path: 'nutrition',
        title: 'Nutrition · FitTrack',
        loadChildren: () => import('./features/nutrition/nutrition.module').then((m) => m.NutritionModule),
      },
      {
        path: 'videos',
        title: 'Exercise Videos · FitTrack',
        loadChildren: () => import('./features/videos/videos.module').then((m) => m.VideosModule),
      },
      {
        path: 'reminders',
        title: 'Reminders · FitTrack',
        loadChildren: () => import('./features/reminders/reminders.module').then((m) => m.RemindersModule),
      },
      // Weight & body measurements arrive in Phase 6 - clearly labelled placeholder, no fake data.
      { path: 'progress', component: ComingSoonComponent, title: 'Progress · FitTrack', data: { title: 'Progress', icon: 'trending_up', phase: 6 } },
    ],
  },
  { path: '**', redirectTo: '' },
];

@NgModule({
  imports: [
    RouterModule.forRoot(routes, {
      scrollPositionRestoration: 'top',
      anchorScrolling: 'enabled',
      bindToComponentInputs: true,
    }),
  ],
  exports: [RouterModule],
})
export class AppRoutingModule {}
