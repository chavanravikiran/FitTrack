import { Gender } from './profile.models';

export type FitnessLevel = 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED';
export type PrimaryGoal = 'WEIGHT_LOSS' | 'MUSCLE_GAIN' | 'MAINTENANCE' | 'GENERAL_FITNESS';
export type WorkoutLocation = 'HOME' | 'GYM';
export type DietPreference = 'VEGETARIAN' | 'NON_VEGETARIAN' | 'VEGAN' | 'EGGETARIAN';
export type ActivityLevel = 'SEDENTARY' | 'LIGHTLY_ACTIVE' | 'MODERATELY_ACTIVE' | 'VERY_ACTIVE';
export type BodyArea = 'KNEE' | 'LOWER_BACK' | 'SHOULDER' | 'WRIST' | 'ANKLE' | 'NECK' | 'HIP' | 'ELBOW';
export type LimitationSeverity = 'MILD' | 'MODERATE' | 'SEVERE';
export type DayOfWeek = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';

export interface Equipment {
  code: string;
  name: string;
}

export interface Limitation {
  bodyArea: BodyArea;
  severity: LimitationSeverity;
}

export interface AssessmentRequest {
  fitnessLevel: FitnessLevel;
  primaryGoal: PrimaryGoal;
  workoutLocation: WorkoutLocation;
  availableDays: DayOfWeek[];
  sessionMinutes: number;
  equipmentCodes: string[];
  dietPreference: DietPreference;
  allergies: string | null;
  dietaryRestrictions: string | null;
  injuryNotes: string | null;
  limitations: Limitation[];
  activityLevel: ActivityLevel;
  sleepHours: number;
}

export interface Assessment {
  id: number;
  fitnessLevel: FitnessLevel;
  primaryGoal: PrimaryGoal;
  workoutLocation: WorkoutLocation;
  availableDays: DayOfWeek[];
  sessionMinutes: number;
  equipment: Equipment[];
  dietPreference: DietPreference;
  allergies: string | null;
  dietaryRestrictions: string | null;
  injuryNotes: string | null;
  limitations: Limitation[];
  activityLevel: ActivityLevel;
  sleepHours: number;
  current: boolean;
  createdAt: string;
}

export interface AssessmentSummary {
  id: number;
  fitnessLevel: FitnessLevel;
  primaryGoal: PrimaryGoal;
  workoutLocation: WorkoutLocation;
  daysPerWeek: number;
  current: boolean;
  createdAt: string;
}

export interface OnboardingRequest {
  fullName: string;
  dateOfBirth: string;
  gender: Gender;
  heightCm: number;
  currentWeightKg: number;
  targetWeightKg: number;
  timeZone: string;
  assessment: AssessmentRequest;
}
