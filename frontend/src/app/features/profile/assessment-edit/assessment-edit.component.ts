import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder } from '@angular/forms';
import { finalize } from 'rxjs';
import { Assessment, AssessmentSummary } from '../../../core/models/assessment.models';
import { AssessmentService } from '../../../core/services/assessment.service';
import { NotifyService } from '../../../core/services/notify.service';
import { apiErrorMessage, applyServerErrors } from '../../../core/utils/api-error';
import { FITNESS_LEVEL_OPTIONS, GOAL_OPTIONS, labelOf } from '../../../shared/constants/options';
import { AssessmentForm, buildAssessmentForm, toAssessmentRequest } from '../../../shared/forms/assessment-form';

/** Re-assessment: every save creates a new version so past plans stay explainable. */
@Component({
  selector: 'ft-assessment-edit',
  standalone: false,
  template: `
    @if (form) {
      <div class="layout">
        <mat-card appearance="outlined" class="ft-card">
          <div class="head">
            <div>
              <h2 class="ft-section-title">Fitness assessment</h2>
              @if (current) {
                <p class="ft-muted sub">Last updated {{ current.createdAt | date: 'medium' }}. Update it whenever your goals, schedule, equipment or health change.</p>
              }
            </div>
          </div>
          @if (error) {
            <div class="alert" role="alert"><mat-icon>error</mat-icon> {{ error }}</div>
          }
          <ft-assessment-form [form]="form" section="all" />
          <div class="actions">
            <button mat-button type="button" (click)="reset()" [disabled]="saving || form.pristine">Discard changes</button>
            <button mat-flat-button type="button" (click)="save()" [disabled]="saving || form.pristine">
              @if (saving) { <mat-spinner diameter="20" /> } @else { Save assessment }
            </button>
          </div>
        </mat-card>

        <mat-card appearance="outlined" class="ft-card history">
          <h2 class="ft-section-title">History</h2>
          @if (history.length) {
            <mat-list>
              @for (h of history; track h.id) {
                <mat-list-item>
                  <mat-icon matListItemIcon aria-hidden="true">{{ h.current ? 'radio_button_checked' : 'history' }}</mat-icon>
                  <span matListItemTitle>{{ label(goals, h.primaryGoal) }} · {{ label(levels, h.fitnessLevel) }}</span>
                  <span matListItemLine class="ft-muted">{{ h.createdAt | date: 'mediumDate' }} · {{ h.daysPerWeek }} days/week{{ h.current ? ' · current' : '' }}</span>
                </mat-list-item>
              }
            </mat-list>
          } @else {
            <p class="ft-muted">No previous versions.</p>
          }
        </mat-card>
      </div>
    } @else {
      <mat-progress-bar mode="indeterminate" />
    }
  `,
  styles: `
    .layout { display: grid; grid-template-columns: 1fr; gap: 16px; }
    @media (min-width: 1100px) { .layout { grid-template-columns: 1fr 300px; align-items: start; } }
    .sub { margin: -6px 0 16px; font: var(--mat-sys-body-small); }
    .actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 20px; }
    .alert { display: flex; gap: 8px; padding: 12px; border-radius: 12px; margin-bottom: 12px; background: var(--mat-sys-error-container); color: var(--mat-sys-on-error-container); }
  `,
})
export class AssessmentEditComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  form: AssessmentForm | null = null;
  current: Assessment | null = null;
  history: AssessmentSummary[] = [];
  saving = false;
  error: string | null = null;

  readonly label = labelOf;
  readonly goals = GOAL_OPTIONS;
  readonly levels = FITNESS_LEVEL_OPTIONS;

  constructor(
    private readonly assessments: AssessmentService,
    private readonly notify: NotifyService,
  ) {}

  ngOnInit(): void {
    this.assessments.current().subscribe((a) => {
      this.current = a;
      this.form = buildAssessmentForm(this.fb, a);
    });
    this.loadHistory();
  }

  reset(): void {
    this.form = buildAssessmentForm(this.fb, this.current);
    this.error = null;
  }

  save(): void {
    if (!this.form) return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.error = 'Please fix the highlighted fields.';
      return;
    }
    this.saving = true;
    this.error = null;
    this.assessments
      .reassess(toAssessmentRequest(this.form))
      .pipe(finalize(() => (this.saving = false)))
      .subscribe({
        next: (a) => {
          this.current = a;
          this.form = buildAssessmentForm(this.fb, a);
          this.loadHistory();
          this.notify.success('Assessment saved. Your recommendations will use the new details.');
        },
        error: (err) => {
          if (this.form) applyServerErrors(this.form, err);
          this.error = apiErrorMessage(err);
        },
      });
  }

  private loadHistory(): void {
    this.assessments.history(0, 10).subscribe((page) => (this.history = page.content));
  }
}
