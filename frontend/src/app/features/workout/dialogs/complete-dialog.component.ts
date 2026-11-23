import { Component, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

export interface CompleteResult {
  effort: number | null;
  notes: string | null;
}

@Component({
  selector: 'ft-complete-dialog',
  standalone: false,
  template: `
    <h2 mat-dialog-title>Finish workout</h2>
    <mat-dialog-content>
      @if (progress < 100) {
        <p class="ft-muted">You've completed {{ progress }}% of your sets - that still counts. Every session builds the habit.</p>
      } @else {
        <p>All sets done - brilliant work!</p>
      }
      <label class="label" id="effort-label">How hard did it feel overall? <strong>{{ effort }}/10</strong></label>
      <mat-slider min="1" max="10" step="1" discrete showTickMarks aria-labelledby="effort-label" class="slider">
        <input matSliderThumb [(ngModel)]="effort" />
      </mat-slider>
      <p class="ft-muted small">{{ hint }}</p>
      <mat-form-field appearance="outline" class="ft-full-width">
        <mat-label>Notes (optional)</mat-label>
        <textarea matInput rows="2" maxlength="1000" [(ngModel)]="notes" placeholder="e.g. felt strong, knee was fine"></textarea>
      </mat-form-field>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Keep going</button>
      <button mat-flat-button (click)="done()"><mat-icon>flag</mat-icon> Finish</button>
    </mat-dialog-actions>
  `,
  styles: `.label { display: block; margin-top: 8px; } .slider { width: 100%; } .small { font: var(--mat-sys-body-small); margin-top: 0; }`,
})
export class CompleteDialogComponent {
  readonly progress = inject<number>(MAT_DIALOG_DATA);
  effort = 6;
  notes = '';

  constructor(private readonly ref: MatDialogRef<CompleteDialogComponent, CompleteResult>) {}

  get hint(): string {
    if (this.effort <= 4) return 'Easy - next time you can add a little more.';
    if (this.effort <= 7) return 'Just right for steady progress.';
    if (this.effort <= 8) return 'Challenging - good, but make sure you recover.';
    return 'Very hard - we will suggest keeping the same load next time.';
  }

  done(): void {
    this.ref.close({ effort: this.effort, notes: this.notes.trim() || null });
  }
}
