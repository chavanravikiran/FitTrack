import {
  ActivityLevel,
  BodyArea,
  DayOfWeek,
  DietPreference,
  FitnessLevel,
  LimitationSeverity,
  PrimaryGoal,
  WorkoutLocation,
} from '../../core/models/assessment.models';
import { GoalType } from '../../core/models/goal.models';
import { Gender } from '../../core/models/profile.models';

export interface Option<T> {
  value: T;
  label: string;
  hint?: string;
  icon?: string;
}

export const GENDER_OPTIONS: Option<Gender>[] = [
  { value: 'FEMALE', label: 'Female' },
  { value: 'MALE', label: 'Male' },
  { value: 'PREFER_NOT_TO_SAY', label: 'Prefer not to say' },
];

export const FITNESS_LEVEL_OPTIONS: Option<FitnessLevel>[] = [
  { value: 'BEGINNER', label: 'Beginner', hint: 'New to exercise or returning after a long break', icon: 'directions_walk' },
  { value: 'INTERMEDIATE', label: 'Intermediate', hint: 'Training regularly for 6+ months', icon: 'directions_run' },
  { value: 'ADVANCED', label: 'Advanced', hint: 'Consistent structured training for 2+ years', icon: 'sprint' },
];

export const GOAL_OPTIONS: Option<PrimaryGoal>[] = [
  { value: 'WEIGHT_LOSS', label: 'Lose weight', hint: 'Reduce body fat gradually and sustainably', icon: 'monitor_weight' },
  { value: 'MUSCLE_GAIN', label: 'Build muscle', hint: 'Get stronger and add lean muscle', icon: 'fitness_center' },
  { value: 'MAINTENANCE', label: 'Maintain', hint: 'Keep your current weight and stay healthy', icon: 'balance' },
  { value: 'GENERAL_FITNESS', label: 'Get fitter', hint: 'More energy, stamina and mobility', icon: 'favorite' },
];

export const LOCATION_OPTIONS: Option<WorkoutLocation>[] = [
  { value: 'HOME', label: 'At home', icon: 'home' },
  { value: 'GYM', label: 'At the gym', icon: 'exercise' },
];

export const DIET_OPTIONS: Option<DietPreference>[] = [
  { value: 'VEGETARIAN', label: 'Vegetarian' },
  { value: 'EGGETARIAN', label: 'Vegetarian + eggs' },
  { value: 'NON_VEGETARIAN', label: 'Non-vegetarian' },
  { value: 'VEGAN', label: 'Vegan' },
];

export const ACTIVITY_OPTIONS: Option<ActivityLevel>[] = [
  { value: 'SEDENTARY', label: 'Mostly sitting', hint: 'Desk job, little walking' },
  { value: 'LIGHTLY_ACTIVE', label: 'Lightly active', hint: 'Some walking or standing during the day' },
  { value: 'MODERATELY_ACTIVE', label: 'Moderately active', hint: 'On your feet much of the day' },
  { value: 'VERY_ACTIVE', label: 'Very active', hint: 'Physical job or very active lifestyle' },
];

export const DAY_OPTIONS: Option<DayOfWeek>[] = [
  { value: 'MONDAY', label: 'Mon' },
  { value: 'TUESDAY', label: 'Tue' },
  { value: 'WEDNESDAY', label: 'Wed' },
  { value: 'THURSDAY', label: 'Thu' },
  { value: 'FRIDAY', label: 'Fri' },
  { value: 'SATURDAY', label: 'Sat' },
  { value: 'SUNDAY', label: 'Sun' },
];

export const BODY_AREA_OPTIONS: Option<BodyArea>[] = [
  { value: 'KNEE', label: 'Knee' },
  { value: 'LOWER_BACK', label: 'Lower back' },
  { value: 'SHOULDER', label: 'Shoulder' },
  { value: 'NECK', label: 'Neck' },
  { value: 'WRIST', label: 'Wrist' },
  { value: 'ELBOW', label: 'Elbow' },
  { value: 'HIP', label: 'Hip' },
  { value: 'ANKLE', label: 'Ankle' },
];

export const SEVERITY_OPTIONS: Option<LimitationSeverity>[] = [
  { value: 'MILD', label: 'Mild' },
  { value: 'MODERATE', label: 'Moderate' },
  { value: 'SEVERE', label: 'Severe' },
];

export const SESSION_MINUTE_OPTIONS = [15, 20, 30, 45, 60, 75, 90];

export const GOAL_TYPE_OPTIONS: (Option<GoalType> & { unit: string; min: number; max: number; step: number })[] = [
  { value: 'TARGET_WEIGHT', label: 'Target weight', unit: 'kg', min: 30, max: 300, step: 0.5, icon: 'monitor_weight' },
  { value: 'WEEKLY_WORKOUTS', label: 'Workouts per week', unit: 'sessions', min: 1, max: 6, step: 1, icon: 'fitness_center' },
  { value: 'DAILY_STEPS', label: 'Daily steps', unit: 'steps', min: 1000, max: 30000, step: 500, icon: 'directions_walk' },
  { value: 'DAILY_WATER', label: 'Daily water', unit: 'ml', min: 500, max: 6000, step: 250, icon: 'water_drop' },
  { value: 'SLEEP_HOURS', label: 'Sleep', unit: 'hours', min: 5, max: 12, step: 0.5, icon: 'bedtime' },
  { value: 'BODY_FAT', label: 'Body fat', unit: '%', min: 5, max: 50, step: 0.5, icon: 'percent' },
];

/** Looks up a human label for an enum value. */
export function labelOf<T>(options: Option<T>[], value: T | null | undefined): string {
  return options.find((o) => o.value === value)?.label ?? (value == null ? '—' : String(value));
}
