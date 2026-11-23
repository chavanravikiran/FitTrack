import { Component, Input } from '@angular/core';

export interface BarDatum {
  label: string;
  value: number;
  /** Optional per-bar target drawn as a marker. */
  goal?: number;
  tooltip?: string;
}

/**
 * Lightweight, accessible bar chart (no chart library). Bars that reach their goal use the primary colour.
 * The full charting dashboard (Chart.js) arrives in Phase 7.
 */
@Component({
  selector: 'ft-bar-chart',
  standalone: false,
  template: `
    <figure class="chart" [style.height.px]="height">
      <figcaption class="cdk-visually-hidden">{{ ariaLabel }}</figcaption>
      <div class="bars" role="list">
        @for (d of data; track d.label) {
          <div class="col" role="listitem" [matTooltip]="d.tooltip ?? (d.label + ': ' + d.value + (unit ? ' ' + unit : ''))"
               [attr.aria-label]="d.label + ': ' + d.value + (unit ? ' ' + unit : '')">
            <div class="track">
              @if (d.goal) {
                <div class="goal" [style.bottom.%]="pct(d.goal)"></div>
              }
              <div class="bar" [class.bar--met]="d.goal ? d.value >= d.goal : true" [style.height.%]="pct(d.value)"></div>
            </div>
            @if (showLabels) {
              <span class="label">{{ d.label }}</span>
            }
          </div>
        }
      </div>
    </figure>
  `,
  styles: `
    .chart { margin: 0; display: flex; flex-direction: column; }
    .bars { flex: 1; display: flex; align-items: stretch; gap: 6px; }
    .col { flex: 1; display: flex; flex-direction: column; align-items: center; min-width: 0; }
    .track { position: relative; flex: 1; width: 100%; display: flex; align-items: flex-end; }
    .bar { width: 100%; min-height: 2px; border-radius: 6px 6px 2px 2px; background: var(--mat-sys-secondary); opacity: .55; transition: height .3s; }
    .bar--met { background: var(--mat-sys-primary); opacity: .9; }
    .goal { position: absolute; left: -2px; right: -2px; border-top: 2px dashed var(--mat-sys-tertiary); }
    .label { margin-top: 4px; font: var(--mat-sys-label-small); color: var(--mat-sys-on-surface-variant); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 100%; }
  `,
})
export class BarChartComponent {
  @Input() data: BarDatum[] = [];
  @Input() height = 160;
  @Input() unit = '';
  @Input() showLabels = true;
  @Input() ariaLabel = 'Bar chart';

  private get max(): number {
    return Math.max(1, ...this.data.map((d) => Math.max(d.value, d.goal ?? 0))) * 1.08;
  }

  pct(v: number): number {
    return Math.min(100, (v / this.max) * 100);
  }
}
