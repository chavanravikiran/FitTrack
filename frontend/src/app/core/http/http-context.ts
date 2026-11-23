import { HttpContext, HttpContextToken } from '@angular/common/http';

/** Don't attempt a token refresh + retry when this request returns 401. */
export const SKIP_AUTH_REFRESH = new HttpContextToken<boolean>(() => false);

/** Don't show the global error snackbar; the caller handles errors itself. */
export const SKIP_ERROR_TOAST = new HttpContextToken<boolean>(() => false);

/** Don't drive the global progress bar (e.g. background polling). */
export const SKIP_LOADING = new HttpContextToken<boolean>(() => false);

export function silentContext(): HttpContext {
  return new HttpContext().set(SKIP_AUTH_REFRESH, true).set(SKIP_ERROR_TOAST, true).set(SKIP_LOADING, true);
}
