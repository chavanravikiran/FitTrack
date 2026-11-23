import { Component, OnInit, inject } from '@angular/core';
import { AbstractControl, FormBuilder, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Observable, finalize, map, startWith } from 'rxjs';
import { Gender } from '../../core/models/profile.models';
import { AuthService } from '../../core/services/auth.service';
import { NotifyService } from '../../core/services/notify.service';
import { ProfileService } from '../../core/services/profile.service';
import { apiErrorMessage, apiFieldErrors } from '../../core/utils/api-error';
import {
  ACTIVITY_OPTIONS,
  DAY_OPTIONS,
  DIET_OPTIONS,
  FITNESS_LEVEL_OPTIONS,
  GENDER_OPTIONS,
  GOAL_OPTIONS,
  LOCATION_OPTIONS,
  labelOf,
} from '../../shared/constants/options';
import { buildAssessmentForm, toAssessmentRequest } from '../../shared/forms/assessment-form';
import { browserTimeZone, minHealthyWeight, toIsoDate } from '../../shared/forms/validators';

/** Target weight must keep BMI >= 18.5 and be consistent with the chosen goal. */
const safeTargetValidator: ValidatorFn = (group: AbstractControl): ValidationErrors | null => {
  const height = Number(group.get('heightCm')?.value);
  const target = group.get('targetWeightKg');
  if (!target || !height || target.value == null) return null;
  const min = minHealthyWeight(height);
  const errors = { ...(target.errors ?? {}) };
  if (Number(target.value) < min) {
    errors['unsafeTarget'] = `For your height, aim for at least ${min} kg (BMI 18.5)`;
  } else {
    delete errors['unsafeTarget'];
  }
  target.setErrors(Object.keys(errors).length ? errors : null);
  return null;
};

@Component({
  selector: 'ft-onboarding',
  standalone: false,
  templateUrl: './onboarding.component.html',
  styleUrl: './onboarding.component.scss',
})
export class OnboardingComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  readonly genders = GENDER_OPTIONS;
  readonly maxDob = this.yearsAgo(16);
  readonly minDob = this.yearsAgo(100);
  readonly timeZones: string[] = this.supportedTimeZones();

  readonly aboutForm = this.fb.group({
    fullName: this.fb.nonNullable.control(this.auth.currentUser?.fullName ?? '', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]),
    dateOfBirth: this.fb.control<Date | null>(null, Validators.required),
    gender: this.fb.control<Gender | null>(null, Validators.required),
    timeZone: this.fb.nonNullable.control(browserTimeZone(), [Validators.required, (c: AbstractControl) => this.timeZones.includes(c.value) ? null : { pattern: true }]),
  });

  readonly bodyForm = this.fb.group(
    {
      heightCm: this.fb.control<number | null>(null, [Validators.required, Validators.min(90), Validators.max(250)]),
      currentWeightKg: this.fb.control<number | null>(null, [Validators.required, Validators.min(20), Validators.max(400)]),
      targetWeightKg: this.fb.control<number | null>(null, [Validators.required, Validators.min(30), Validators.max(300)]),
    },
    { validators: safeTargetValidator },
  );

  readonly assessmentForm = buildAssessmentForm(this.fb);
  readonly acknowledge = this.fb.nonNullable.control(false, Validators.requiredTrue);

  filteredZones$!: Observable<string[]>;
  submitting = false;
  submitError: string | null = null;

  constructor(
    private readonly profileService: ProfileService,
    private readonly notify: NotifyService,
    private readonly router: Router,
  ) {}

  ngOnInit(): void {
    this.filteredZones$ = this.aboutForm.controls.timeZone.valueChanges.pipe(
      startWith(this.aboutForm.controls.timeZone.value),
      map((q) => {
        const query = (q ?? '').toLowerCase().replace(/\s+/g, '_');
        return this.timeZones.filter((z) => z.toLowerCase().includes(query)).slice(0, 50);
      }),
    );
  }

  // ---- Step validity -------------------------------------------------------
  get goalsStepValid(): boolean {
    const c = this.assessmentForm.controls;
    return c.primaryGoal.valid && c.fitnessLevel.valid && c.workoutLocation.valid;
  }

  get scheduleStepValid(): boolean {
    const c = this.assessmentForm.controls;
    return c.availableDays.valid && c.sessionMinutes.valid && c.equipmentCodes.valid;
  }

  get healthStepValid(): boolean {
    const c = this.assessmentForm.controls;
    return c.activityLevel.valid && c.sleepHours.valid && c.dietPreference.valid && c.allergies.valid
      && c.dietaryRestrictions.valid && c.injuryNotes.valid && c.limitations.valid;
  }

  touchGoals(): void {
    const c = this.assessmentForm.controls;
    [c.primaryGoal, c.fitnessLevel, c.workoutLocation].forEach((x) => x.markAsTouched());
  }

  touchHealth(): void {
    const c = this.assessmentForm.controls;
    [c.activityLevel, c.sleepHours, c.dietPreference, c.limitations].forEach((x) => x.markAllAsTouched());
  }

  // ---- Live guidance -------------------------------------------------------
  get bmi(): number | null {
    const h = Number(this.bodyForm.controls.heightCm.value);
    const w = Number(this.bodyForm.controls.currentWeightKg.value);
    if (!h || !w || this.bodyForm.controls.heightCm.invalid || this.bodyForm.controls.currentWeightKg.invalid) return null;
    return Math.round((w / Math.pow(h / 100, 2)) * 10) / 10;
  }

  get bmiCategory(): string {
    const b = this.bmi;
    if (b == null) return '';
    if (b < 18.5) return 'below the healthy range';
    if (b < 25) return 'in the healthy range';
    if (b < 30) return 'above the healthy range';
    return 'well above the healthy range';
  }

  get minTarget(): number | null {
    const h = Number(this.bodyForm.controls.heightCm.value);
    return h && this.bodyForm.controls.heightCm.valid ? minHealthyWeight(h) : null;
  }

  /** Rough, safe timeline at ~0.5 kg/week for loss or ~0.25 kg/week for gain. Educational only. */
  get timelineWeeks(): number | null {
    const cur = Number(this.bodyForm.controls.currentWeightKg.value);
    const tgt = Number(this.bodyForm.controls.targetWeightKg.value);
    if (!cur || !tgt || this.bodyForm.invalid) return null;
    const diff = cur - tgt;
    if (Math.abs(diff) < 0.5) return 0;
    return Math.ceil(diff > 0 ? diff / 0.5 : -diff / 0.25);
  }

  // ---- Review helpers ------------------------------------------------------
  label = labelOf;
  readonly opts = {
    goal: GOAL_OPTIONS,
    level: FITNESS_LEVEL_OPTIONS,
    location: LOCATION_OPTIONS,
    diet: DIET_OPTIONS,
    activity: ACTIVITY_OPTIONS,
    days: DAY_OPTIONS,
    gender: GENDER_OPTIONS,
  };

  get selectedDays(): string {
    return this.assessmentForm.controls.availableDays.value.map((d) => labelOf(DAY_OPTIONS, d)).join(', ');
  }

  // ---- Submit ----------------------------------------------------------------
  submit(): void {
    if (this.aboutForm.invalid || this.bodyForm.invalid || this.assessmentForm.invalid || this.acknowledge.invalid) {
      this.aboutForm.markAllAsTouched();
      this.bodyForm.markAllAsTouched();
      this.assessmentForm.markAllAsTouched();
      this.acknowledge.markAsTouched();
      return;
    }
    const about = this.aboutForm.getRawValue();
    const body = this.bodyForm.getRawValue();
    this.submitting = true;
    this.submitError = null;

    this.profileService
      .completeOnboarding({
        fullName: about.fullName.trim(),
        dateOfBirth: toIsoDate(about.dateOfBirth)!,
        gender: about.gender!,
        heightCm: Number(body.heightCm),
        currentWeightKg: Number(body.currentWeightKg),
        targetWeightKg: Number(body.targetWeightKg),
        timeZone: about.timeZone,
        assessment: toAssessmentRequest(this.assessmentForm),
      })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: (profile) => {
          this.auth.patchUser({ onboardingCompleted: true, fullName: profile.fullName });
          this.notify.success('Your profile is ready. Welcome to FitTrack!');
          this.router.navigateByUrl('/dashboard');
        },
        error: (err) => {
          const fields = apiFieldErrors(err).map((f) => f.message);
          this.submitError = [apiErrorMessage(err), ...fields].join(' ');
        },
      });
  }

  signOut(): void {
    this.auth.logout();
  }

  private yearsAgo(years: number): Date {
    const d = new Date();
    d.setFullYear(d.getFullYear() - years);
    return d;
  }

  private supportedTimeZones(): string[] {
    const intl = Intl as unknown as { supportedValuesOf?: (key: string) => string[] };
    const zones = intl.supportedValuesOf?.('timeZone') ?? [];
    const browser = browserTimeZone();
    return zones.includes(browser) ? zones : [browser, ...zones, 'UTC'];
  }
}
