// Note: the API omits null fields, so optional properties may be absent (undefined).

export type Difficulty = 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED';
export type DayStatus = 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED' | 'SKIPPED' | 'RESCHEDULED' | 'MISSED' | 'REST';

export interface MuscleGroup {
  code: string;
  name: string;
}

export interface ExerciseSummary {
  id: number;
  name: string;
  slug: string;
  muscleGroup: MuscleGroup;
  difficulty: Difficulty;
  location: 'HOME' | 'GYM' | 'BOTH';
  equipment: string[];
  defaultSets?: number;
  defaultReps?: number;
  defaultDurationSec?: number;
  defaultRestSec?: number;
  durationBased: boolean;
}

export interface ExerciseDetail {
  summary: ExerciseSummary;
  description?: string;
  instructions?: string;
  commonMistakes?: string;
  safetyPrecautions?: string;
  secondaryMuscles: MuscleGroup[];
  avoidIf: string[];
  alternatives: ExerciseSummary[];
  videoId?: number;
}

export interface PlanExercise {
  id: number;
  exercise: ExerciseSummary;
  sets: number;
  repsMin?: number;
  repsMax?: number;
  durationSec?: number;
  restSec: number;
  targetRpe?: number;
  notes?: string;
}

export interface PlanDay {
  id: number;
  dayOfWeek: number;
  title: string;
  focus?: string;
  restDay: boolean;
  exercises: PlanExercise[];
}

export interface WorkoutPlan {
  id: number;
  name: string;
  splitType: string;
  startDate: string;
  explanations: string[];
  days: PlanDay[];
}

export interface SetLog {
  setNumber: number;
  repsDone?: number;
  weightKg?: number;
  durationSec?: number;
  completed: boolean;
}

export interface SessionExercise {
  planExerciseId?: number;
  exercise: ExerciseSummary;
  sets: number;
  repsMin?: number;
  repsMax?: number;
  durationSec?: number;
  restSec: number;
  targetRpe?: number;
  notes?: string;
  loggedSets: SetLog[];
  progressionTip?: string;
}

export interface DayWorkout {
  date: string;
  planDayId?: number;
  title: string;
  focus?: string;
  restDay: boolean;
  status: DayStatus;
  sessionId?: number;
  startedAt?: string;
  completedAt?: string;
  perceivedEffort?: number;
  caloriesEst?: number;
  estimatedMinutes: number;
  exercises: SessionExercise[];
  extraExercises: SessionExercise[];
  recoveryNote?: string;
}

export interface CalendarEntry {
  date: string;
  planDayId: number;
  title: string;
  restDay: boolean;
  status: DayStatus;
  sessionId?: number;
  rescheduledTo?: string;
  rescheduledHere: boolean;
}

export interface WorkoutSummary {
  plannedThisWeek: number;
  completedThisWeek: number;
  skippedThisWeek: number;
  streakDays: number;
  totalCompleted: number;
}

export interface HistoryItem {
  sessionId: number;
  date: string;
  title: string;
  status: 'COMPLETED' | 'SKIPPED' | 'RESCHEDULED';
  completedAt?: string;
  perceivedEffort?: number;
  caloriesEst?: number;
  setsCompleted: number;
  notes?: string;
}

export interface PersonalRecord {
  exerciseId: number;
  exerciseName: string;
  type: 'MAX_WEIGHT' | 'MAX_REPS' | 'LONGEST_DURATION';
  value: number;
  achievedOn: string;
}

export interface SetLogRequest {
  exerciseId: number;
  setNumber: number;
  repsDone?: number | null;
  weightKg?: number | null;
  durationSec?: number | null;
  completed: boolean;
}
