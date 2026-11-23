import { HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { NgModule, Optional, SkipSelf, inject, provideAppInitializer } from '@angular/core';
import { MatIconRegistry } from '@angular/material/icon';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { AuthInterceptor } from './interceptors/auth.interceptor';
import { ErrorInterceptor } from './interceptors/error.interceptor';
import { LoadingInterceptor } from './interceptors/loading.interceptor';
import { AuthService } from './services/auth.service';
import { ThemeService } from './services/theme.service';

/**
 * App-wide singletons: HTTP pipeline, interceptors and start-up session restore.
 * Import once, in AppModule.
 */
@NgModule({
  imports: [MatSnackBarModule],
  providers: [
    provideHttpClient(withInterceptorsFromDi()),
    // Order matters: loading wraps everything, auth retries before errors are reported.
    { provide: HTTP_INTERCEPTORS, useClass: LoadingInterceptor, multi: true },
    { provide: HTTP_INTERCEPTORS, useClass: ErrorInterceptor, multi: true },
    { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true },
    provideAppInitializer(() => {
      inject(ThemeService); // apply saved theme before first paint
      inject(MatIconRegistry).setDefaultFontSetClass('material-symbols-rounded');
      return inject(AuthService).restoreSession();
    }),
  ],
})
export class CoreModule {
  constructor(@Optional() @SkipSelf() parent: CoreModule | null) {
    if (parent) {
      throw new Error('CoreModule is already loaded. Import it in AppModule only.');
    }
  }
}
