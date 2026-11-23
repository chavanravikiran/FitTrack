import { Component, Input, OnInit, inject } from '@angular/core';
import { FormBuilder } from '@angular/forms';
import { Observable } from 'rxjs';
import { Equipment } from '../../../core/models/assessment.models';
import { AssessmentService } from '../../../core/services/assessment.service';
import {
  ACTIVITY_OPTIONS,
  BODY_AREA_OPTIONS,
  DAY_OPTIONS,
  DIET_OPTIONS,
  FITNESS_LEVEL_OPTIONS,
  GOAL_OPTIONS,
  LOCATION_OPTIONS,
  SESSION_MINUTE_OPTIONS,
  SEVERITY_OPTIONS,
} from '../../constants/options';
import { AssessmentForm, buildLimitation } from '../../forms/assessment-form';

export type AssessmentSection = 'goals' | 'schedule' | 'health' | 'all';

/**
 * Renders the fitness-assessment fields for a given section. Used by the onboarding stepper
 * (one section per step) and by the profile "update assessment" page (all sections).
 */
@Component({
  selector: 'ft-assessment-form',
  standalone: false,
  templateUrl: './assessment-form.component.html',
  styleUrl: './assessment-form.component.scss',
})
export class AssessmentFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  @Input({ required: true }) form!: AssessmentForm;
  @Input() section: AssessmentSection = 'all';

  readonly levels = FITNESS_LEVEL_OPTIONS;
  readonly goals = GOAL_OPTIONS;
  readonly locations = LOCATION_OPTIONS;
  readonly diets = DIET_OPTIONS;
  readonly activities = ACTIVITY_OPTIONS;
  readonly days = DAY_OPTIONS;
  readonly bodyAreas = BODY_AREA_OPTIONS;
  readonly severities = SEVERITY_OPTIONS;
  readonly minutes = SESSION_MINUTE_OPTIONS;

  equipment$!: Observable<Equipment[]>;

  constructor(
    private readonly assessmentService: AssessmentService,
  ) {}

  ngOnInit(): void {
    this.equipment$ = this.assessmentService.equipment();
  }

  show(section: Exclude<AssessmentSection, 'all'>): boolean {
    return this.section === 'all' || this.section === section;
  }

  get limitations() {
    return this.form.controls.limitations;
  }

  addLimitation(): void {
    this.limitations.push(buildLimitation(this.fb));
  }

  removeLimitation(index: number): void {
    this.limitations.removeAt(index);
  }

  /** Body areas already chosen in other rows are disabled to prevent duplicates. */
  isAreaTaken(area: string, rowIndex: number): boolean {
    return this.limitations.controls.some((c, i) => i !== rowIndex && c.controls.bodyArea.value === area);
  }

  get daysPerWeek(): number {
    return this.form.controls.availableDays.value.length;
  }
}
