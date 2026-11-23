import { Component, Input } from '@angular/core';

@Component({
  selector: 'ft-progress-ring',
  standalone: false,
  template: `
    <div class="ring" [style.width.px]="size" [style.height.px]="size" role="img"
         [attr.aria-label]="label + ': ' + percent + ' percent'">
      <svg [attr.viewBox]="'0 0 ' + size + ' ' + size">
        <circle class="bg" [attr.cx]="size / 2" [attr.cy]="size / 2" [attr.r]="radius" [attr.stroke-width]="stroke" />
        <circle class="fg" [class.fg--done]="percent >= 100" [attr.cx]="size / 2" [attr.cy]="size / 2" [attr.r]="radius"
                [attr.stroke-width]="stroke" [attr.stroke-dasharray]="circumference"
                [attr.stroke-dashoffset]="circumference * (1 - clamped / 100)" />
      </svg>
      <div class="center"><ng-content /></div>
    </div>
  `,
  styles: `
    .ring { position: relative; display: inline-grid; place-items: center; }
    svg { position: absolute; inset: 0; transform: rotate(-90deg); }
    circle { fill: none; }
    .bg { stroke: var(--mat-sys-surface-container-highest); }
    .fg { stroke: var(--mat-sys-secondary); stroke-linecap: round; transition: stroke-dashoffset .5s ease; }
    .fg--done { stroke: var(--mat-sys-primary); }
    .center { position: relative; text-align: center; display: flex; flex-direction: column; align-items: center; }
  `,
})
export class ProgressRingComponent {
  @Input() percent = 0;
  @Input() size = 160;
  @Input() stroke = 14;
  @Input() label = 'Progress';

  get radius(): number {
    return (this.size - this.stroke) / 2;
  }

  get circumference(): number {
    return 2 * Math.PI * this.radius;
  }

  get clamped(): number {
    return Math.max(0, Math.min(100, this.percent));
  }
}
