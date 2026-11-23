import { AbstractControl, FormArray, FormBuilder, FormControl, FormGroup, ValidationErrors, Validators } from '@angular/forms';
import {
  ActivityLevel,
  Assessment,
  AssessmentRequest,
  BodyArea,
  DayOfWeek,
  DietPreference,
  FitnessLevel,
  LimitationSeverity,
  PrimaryGoal,
  WorkoutLocation,
} from '../../core/models/assessment.models';

export type LimitationForm = FormGroup<{
  bodyArea: FormControl<BodyArea | null>;
  severity: FormControl<LimitationSeverity | null>;
}>;

export type AssessmentForm = FormGroup<{
  fitnessLevel: FormControl<FitnessLevel | null>;
  primaryGoal: FormControl<PrimaryGoal | null>;
  workoutLocation: FormControl<WorkoutLocation | null>;
  availableDays: FormControl<DayOfWeek[]>;
  sessionMinutes: FormControl<number | null>;
  equipmentCodes: FormControl<string[]>;
  dietPreference: FormControl<DietPreference | null>;
  allergies: FormControl<string | null>;
  dietaryRestrictions: FormControl<string | null>;
  injuryNotes: FormControl<string | null>;
  limitations: FormArray<LimitationForm>;
  activityLevel: FormControl<ActivityLevel | null>;
  sleepHours: FormControl<number | null>;
}>;

function atLeastOne(control: AbstractControl): ValidationErrors | null {
  const v = control.value as unknown[] | null;
  return v && v.length > 0 ? null : { required: true };
}

/** Builds the typed assessment form, mirroring the backend AssessmentRequest validation. */
export function buildAssessmentForm(fb: FormBuilder, a?: Assessment | null): AssessmentForm {
  const form: AssessmentForm = fb.group({
    fitnessLevel: fb.control<FitnessLevel | null>(a?.fitnessLevel ?? 'BEGINNER', Validators.required),
    primaryGoal: fb.control<PrimaryGoal | null>(a?.primaryGoal ?? null, Validators.required),
    workoutLocation: fb.control<WorkoutLocation | null>(a?.workoutLocation ?? 'HOME', Validators.required),
    availableDays: fb.nonNullable.control<DayOfWeek[]>(a?.availableDays ?? ['MONDAY', 'WEDNESDAY', 'FRIDAY'], atLeastOne),
    sessionMinutes: fb.control<number | null>(a?.sessionMinutes ?? 30, [Validators.required, Validators.min(10), Validators.max(180)]),
    equipmentCodes: fb.nonNullable.control<string[]>(a?.equipment.map((e) => e.code) ?? []),
    dietPreference: fb.control<DietPreference | null>(a?.dietPreference ?? null, Validators.required),
    allergies: fb.control<string | null>(a?.allergies ?? null, Validators.maxLength(500)),
    dietaryRestrictions: fb.control<string | null>(a?.dietaryRestrictions ?? null, Validators.maxLength(500)),
    injuryNotes: fb.control<string | null>(a?.injuryNotes ?? null, Validators.maxLength(1000)),
    limitations: fb.array<LimitationForm>((a?.limitations ?? []).map((l) => buildLimitation(fb, l.bodyArea, l.severity))),
    activityLevel: fb.control<ActivityLevel | null>(a?.activityLevel ?? null, Validators.required),
    sleepHours: fb.control<number | null>(a?.sleepHours ?? 7, [Validators.required, Validators.min(3), Validators.max(14)]),
  });
  return form;
}

export function buildLimitation(fb: FormBuilder, bodyArea: BodyArea | null = null, severity: LimitationSeverity | null = 'MILD'): LimitationForm {
  return fb.group({
    bodyArea: fb.control<BodyArea | null>(bodyArea, Validators.required),
    severity: fb.control<LimitationSeverity | null>(severity, Validators.required),
  });
}

export function toAssessmentRequest(form: AssessmentForm): AssessmentRequest {
  const v = form.getRawValue();
  const blank = (s: string | null) => (s && s.trim() ? s.trim() : null);
  return {
    fitnessLevel: v.fitnessLevel!,
    primaryGoal: v.primaryGoal!,
    workoutLocation: v.workoutLocation!,
    availableDays: v.availableDays,
    sessionMinutes: Number(v.sessionMinutes),
    equipmentCodes: v.equipmentCodes,
    dietPreference: v.dietPreference!,
    allergies: blank(v.allergies),
    dietaryRestrictions: blank(v.dietaryRestrictions),
    injuryNotes: blank(v.injuryNotes),
    limitations: v.limitations.map((l) => ({ bodyArea: l.bodyArea!, severity: l.severity! })),
    activityLevel: v.activityLevel!,
    sleepHours: Number(v.sleepHours),
  };
}
