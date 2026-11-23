import { Component, Input } from '@angular/core';

export type StatTone = 'primary' | 'tertiary' | 'secondary' | 'neutral';

@Component({
  selector: 'ft-stat-card',
  standalone: false,
  template: `
    <mat-card class="stat" [class]="'stat--' + tone" appearance="outlined">
      <div class="stat__icon" aria-hidden="true"><mat-icon>{{ icon }}</mat-icon></div>
      <div class="stat__body">
        <span class="stat__label">{{ label }}</span>
        <span class="stat__value">
          {{ value ?? '—' }}
          @if (unit && value !== null && value !== undefined) {
            <small>{{ unit }}</small>
          }
        </span>
        @if (hint) {
          <span class="stat__hint">{{ hint }}</span>
        }
      </div>
    </mat-card>
  `,
  styles: `
    .stat { display: flex; flex-direction: row; align-items: center; gap: 14px; padding: 16px; height: 100%; box-sizing: border-box; }
    .stat__icon { flex: none; width: 44px; height: 44px; border-radius: 14px; display: grid; place-items: center; }
    .stat--primary .stat__icon { background: var(--mat-sys-primary-container); color: var(--mat-sys-on-primary-container); }
    .stat--tertiary .stat__icon { background: var(--mat-sys-tertiary-container); color: var(--mat-sys-on-tertiary-container); }
    .stat--secondary .stat__icon { background: var(--mat-sys-secondary-container); color: var(--mat-sys-on-secondary-container); }
    .stat--neutral .stat__icon { background: var(--mat-sys-surface-container-highest); color: var(--mat-sys-on-surface-variant); }
    .stat__body { display: flex; flex-direction: column; min-width: 0; }
    .stat__label { font: var(--mat-sys-label-medium); color: var(--mat-sys-on-surface-variant); }
    .stat__value { font: var(--mat-sys-title-large); font-weight: 600; font-variant-numeric: tabular-nums; }
    .stat__value small { font: var(--mat-sys-label-medium); color: var(--mat-sys-on-surface-variant); margin-left: 2px; }
    .stat__hint { font: var(--mat-sys-body-small); color: var(--mat-sys-on-surface-variant); }
  `,
})
export class StatCardComponent {
  @Input({ required: true }) label!: string;
  @Input() value: string | number | null | undefined;
  @Input() unit?: string;
  @Input() hint?: string;
  @Input() icon = 'insights';
  @Input() tone: StatTone = 'primary';
}
