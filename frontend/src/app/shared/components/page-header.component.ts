import { Component, Input } from '@angular/core';

@Component({
  selector: 'ft-page-header',
  standalone: false,
  template: `
    <header class="page-header">
      <div class="page-header__text">
        <h1 class="page-header__title">
          @if (icon) {
            <mat-icon aria-hidden="true">{{ icon }}</mat-icon>
          }
          {{ title }}
        </h1>
        @if (subtitle) {
          <p class="page-header__subtitle">{{ subtitle }}</p>
        }
      </div>
      <div class="page-header__actions"><ng-content /></div>
    </header>
  `,
  styles: `
    .page-header { display: flex; flex-wrap: wrap; gap: 12px; align-items: flex-end; justify-content: space-between; margin-bottom: 20px; }
    .page-header__title { display: flex; align-items: center; gap: 10px; margin: 0; font: var(--mat-sys-headline-small); font-weight: 600; }
    .page-header__title mat-icon { color: var(--mat-sys-primary); }
    .page-header__subtitle { margin: 4px 0 0; color: var(--mat-sys-on-surface-variant); font: var(--mat-sys-body-medium); }
    .page-header__actions { display: flex; gap: 8px; flex-wrap: wrap; }
  `,
})
export class PageHeaderComponent {
  @Input({ required: true }) title!: string;
  @Input() subtitle?: string;
  @Input() icon?: string;
}
