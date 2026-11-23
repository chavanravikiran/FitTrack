export type GoalType = 'TARGET_WEIGHT' | 'WEEKLY_WORKOUTS' | 'DAILY_STEPS' | 'DAILY_WATER' | 'SLEEP_HOURS' | 'BODY_FAT';
export type GoalStatus = 'ACTIVE' | 'ACHIEVED' | 'ABANDONED';

export interface Goal {
  id: number;
  goalType: GoalType;
  title: string;
  startValue: number | null;
  targetValue: number;
  unit: string;
  startDate: string;
  targetDate: string | null;
  status: GoalStatus;
  achievedAt: string | null;
}

export interface GoalRequest {
  goalType: GoalType;
  title?: string | null;
  targetValue: number;
  targetDate?: string | null;
}

export interface GoalUpdateRequest {
  title?: string | null;
  targetValue: number;
  targetDate?: string | null;
}
