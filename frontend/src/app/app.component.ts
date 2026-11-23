import { Component } from '@angular/core';
import { LoadingService } from './core/services/loading.service';

@Component({
  selector: 'ft-root',
  standalone: false,
  template: `
    @if (loading.loading$ | async) {
      <mat-progress-bar class="global-progress" mode="indeterminate" aria-label="Loading" />
    }
    <router-outlet />
  `,
  styles: `
    :host { display: block; height: 100%; }
    .global-progress { position: fixed; top: 0; left: 0; right: 0; z-index: 1000; }
  `,
})
export class AppComponent {
  constructor(readonly loading: LoadingService) {}
}
