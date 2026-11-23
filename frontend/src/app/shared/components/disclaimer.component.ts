import { Component, Input } from '@angular/core';

/** Health & nutrition disclaimer shown wherever estimates or guidance appear. */
@Component({
  selector: 'ft-disclaimer',
  standalone: false,
  template: `
    <aside class="disclaimer" role="note">
      <mat-icon aria-hidden="true">info</mat-icon>
      <p>
        <ng-content />
        @if (!custom) {
          FitTrack provides general fitness education and estimates, not medical advice. Results vary from person to
          person and are never guaranteed. If you have a medical condition, injury, are pregnant, or feel pain, dizziness
          or chest discomfort, stop and consult a qualified health professional.
        }
      </p>
    </aside>
  `,
  styles: `
    .disclaimer { display: flex; gap: 10px; align-items: flex-start; padding: 12px 14px; border-radius: 12px;
      background: var(--mat-sys-surface-container); color: var(--mat-sys-on-surface-variant); }
    .disclaimer mat-icon { flex: none; font-size: 20px; width: 20px; height: 20px; margin-top: 1px; }
    .disclaimer p { margin: 0; font: var(--mat-sys-body-small); }
  `,
})
export class DisclaimerComponent {
  /** Set when projecting custom text instead of the default disclaimer. */
  @Input() custom = false;
}
