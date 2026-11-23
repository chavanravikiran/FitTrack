import { Component, OnInit } from '@angular/core';
import { catchError, forkJoin, of } from 'rxjs';
import { Assessment } from '../../core/models/assessment.models';
import { Goal } from '../../core/models/goal.models';
import { Profile } from '../../core/models/profile.models';
import { AssessmentService } from '../../core/services/assessment.service';
import { GoalService } from '../../core/services/goal.service';
import { Recommendations } from '../../core/models/recommendation.models';
import { RecommendationService } from '../../core/services/recommendation.service';
import { ProfileService } from '../../core/services/profile.service';
import {
  DAY_OPTIONS,
  FITNESS_LEVEL_OPTIONS,
  GOAL_OPTIONS,
  GOAL_TYPE_OPTIONS,
  LOCATION_OPTIONS,
  labelOf,
} from '../../shared/constants/options';

/**
 * Phase 2 dashboard: summarises the saved profile, assessment and goals.
 * The full analytics dashboard with charts arrives in Phase 7.
 */
@Component({
  selector: 'ft-dashboard',
  standalone: false,
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent implements OnInit {
  profile: Profile | null = null;
  assessment: Assessment | null = null;
  goals: Goal[] = [];
  loading = true;

  readonly label = labelOf;
  readonly goalOpts = GOAL_OPTIONS;
  readonly levelOpts = FITNESS_LEVEL_OPTIONS;
  readonly locationOpts = LOCATION_OPTIONS;

  recs: Recommendations | null = null;

  readonly upcoming = [
    { icon: 'fitness_center', title: "Today's workout", text: 'Start your session, log sets and track personal records.', path: '/workout' },
    { icon: 'water_drop', title: 'Water & steps', text: 'Quick-add water and log your steps for the day.', path: '/activity' },
    { icon: 'restaurant', title: 'Log a meal', text: 'Indian-friendly food database with calorie and macro estimates.', path: '/nutrition' },
    { icon: 'smart_display', title: 'Exercise videos', text: 'Learn correct technique with step-by-step guides.', path: '/videos' },
  ];

  constructor(
    private readonly profileService: ProfileService,
    private readonly assessmentService: AssessmentService,
    private readonly goalService: GoalService,
    private readonly recommendations: RecommendationService,
  ) {}

  ngOnInit(): void {
    this.recommendations.get().pipe(catchError(() => of(null))).subscribe((r) => (this.recs = r));
    forkJoin({
      profile: this.profileService.load(),
      assessment: this.assessmentService.current().pipe(catchError(() => of(null))),
      goals: this.goalService.list('ACTIVE').pipe(catchError(() => of([] as Goal[]))),
    }).subscribe({
      next: ({ profile, assessment, goals }) => {
        this.profile = profile;
        this.assessment = assessment;
        this.goals = goals;
        this.loading = false;
      },
      error: () => (this.loading = false),
    });
  }

  get greeting(): string {
    const h = new Date().getHours();
    return h < 12 ? 'Good morning' : h < 17 ? 'Good afternoon' : 'Good evening';
  }

  get firstName(): string {
    return this.profile?.fullName.split(' ')[0] ?? '';
  }

  get weightToGo(): number | null {
    const p = this.profile;
    if (p?.currentWeightKg == null || p.targetWeightKg == null) return null;
    return Math.round(Math.abs(p.currentWeightKg - p.targetWeightKg) * 10) / 10;
  }

  get weightDirection(): 'lose' | 'gain' | 'none' {
    const p = this.profile;
    if (p?.currentWeightKg == null || p.targetWeightKg == null) return 'none';
    const d = p.currentWeightKg - p.targetWeightKg;
    return Math.abs(d) < 0.1 ? 'none' : d > 0 ? 'lose' : 'gain';
  }

  get bmiLabel(): string {
    const b = this.profile?.bmi;
    if (b == null) return '';
    if (b < 18.5) return 'Below healthy range';
    if (b < 25) return 'Healthy range';
    if (b < 30) return 'Above healthy range';
    return 'Well above healthy range';
  }

  get trainingDays(): string {
    return this.assessment?.availableDays.map((d) => labelOf(DAY_OPTIONS, d)).join(' · ') ?? '';
  }

  goalIcon(goal: Goal): string {
    return GOAL_TYPE_OPTIONS.find((o) => o.value === goal.goalType)?.icon ?? 'flag';
  }
}
