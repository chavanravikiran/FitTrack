import { Component, Input } from '@angular/core';

@Component({
  selector: 'ft-empty-state',
  standalone: false,
  template: `
    <div class="empty" role="status">
      <mat-icon aria-hidden="true">{{ icon }}</mat-icon>
      <h2>{{ title }}</h2>
      @if (message) {
        <p>{{ message }}</p>
      }
      <ng-content />
    </div>
  `,
  styles: `
    .empty { display: flex; flex-direction: column; align-items: center; text-align: center; gap: 8px; padding: 40px 16px; color: var(--mat-sys-on-surface-variant); }
    .empty mat-icon { width: 56px; height: 56px; font-size: 56px; color: var(--mat-sys-primary); opacity: .8; }
    .empty h2 { margin: 8px 0 0; font: var(--mat-sys-title-medium); color: var(--mat-sys-on-surface); }
    .empty p { margin: 0; max-width: 46ch; font: var(--mat-sys-body-medium); }
  `,
})
export class EmptyStateComponent {
  @Input() icon = 'inbox';
  @Input({ required: true }) title!: string;
  @Input() message?: string;
}
