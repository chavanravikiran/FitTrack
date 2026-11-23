import { Component, OnInit, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { ExerciseSummary } from '../../../core/models/workout.models';
import { WorkoutService } from '../../../core/services/workout.service';

export interface AlternativesDialogData {
  exercise: ExerciseSummary;
  /** session = just for today; plan = permanent swap (Premium). */
  mode: 'session' | 'plan';
}

@Component({
  selector: 'ft-alternatives-dialog',
  standalone: false,
  template: `
    <h2 mat-dialog-title>{{ data.mode === 'plan' ? 'Swap in your plan' : 'Use an alternative today' }}</h2>
    <mat-dialog-content>
      <p class="ft-muted">Instead of <strong>{{ data.exercise.name }}</strong>. Alternatives work similar muscles; choose one that feels comfortable.</p>
      @if (options === null) {
        <mat-progress-bar mode="indeterminate" />
      } @else if (!options.length) {
        <p>No alternatives are listed for this exercise yet.</p>
      } @else {
        <mat-selection-list [multiple]="false" (selectionChange)="choose($event.options[0].value)">
          @for (o of options; track o.id) {
            <mat-list-option [value]="o">
              <span matListItemTitle>{{ o.name }}</span>
              <span matListItemLine class="ft-muted">{{ o.difficulty | titlecase }} · {{ gear(o) }}</span>
            </mat-list-option>
          }
        </mat-selection-list>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end"><button mat-button mat-dialog-close>Cancel</button></mat-dialog-actions>
  `,
})
export class AlternativesDialogComponent implements OnInit {
  readonly data = inject<AlternativesDialogData>(MAT_DIALOG_DATA);
  options: ExerciseSummary[] | null = null;

  constructor(
    private readonly workouts: WorkoutService,
    private readonly ref: MatDialogRef<AlternativesDialogComponent, ExerciseSummary>,
  ) {}

  ngOnInit(): void {
    this.workouts.exercise(this.data.exercise.id).subscribe((d) => (this.options = d.alternatives));
  }

  gear(o: ExerciseSummary): string {
    const g = o.equipment.filter((e) => e !== 'NONE' && e !== 'YOGA_MAT');
    return g.length ? g.map((e) => e.toLowerCase().replace(/_/g, ' ')).join(', ') : 'no equipment';
  }

  choose(o: ExerciseSummary): void {
    this.ref.close(o);
  }
}
