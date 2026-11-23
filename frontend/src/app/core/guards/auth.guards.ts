import { inject } from '@angular/core';
import { CanActivateFn, CanMatchFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/** Requires a signed-in user; otherwise redirects to login and comes back afterwards. */
export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.isAuthenticated() ? true : router.createUrlTree(['/auth/login'], { queryParams: { returnUrl: state.url } });
};

/** Auth pages are only for signed-out visitors. */
export const guestGuard: CanMatchFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.isAuthenticated() ? router.createUrlTree(['/dashboard']) : true;
};

/** The main app requires a completed onboarding (fitness assessment). Admins may use it without one. */
export const onboardedGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.currentUser?.onboardingCompleted || auth.isAdmin() ? true : router.createUrlTree(['/onboarding']);
};

/**
 * Used with canMatch on the landing page: signed-in visitors fall through to the app shell instead.
 * (Returns false rather than a redirect so the router tries the next matching route.)
 */
export const signedOutOnly: CanMatchFn = () => !inject(AuthService).isAuthenticated();

/** Onboarding is shown only until it's completed. */
export const needsOnboardingGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (!auth.isAuthenticated()) {
    return router.createUrlTree(['/auth/login']);
  }
  return auth.currentUser?.onboardingCompleted ? router.createUrlTree(['/dashboard']) : true;
};

export const adminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.isAdmin() ? true : router.createUrlTree(['/dashboard']);
};
