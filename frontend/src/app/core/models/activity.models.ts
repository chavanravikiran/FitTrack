export interface ActivityDay {
  date: string;
  steps: number;
  distanceM?: number;
  activeMinutes?: number;
  caloriesEst?: number;
  stepGoal: number;
  source?: 'MANUAL' | 'HEALTH_CONNECT' | 'APPLE_HEALTH' | 'IMPORT';
  goalMet: boolean;
}

export interface ActivitySummary {
  days: number;
  totalSteps: number;
  averageSteps: number;
  goalMetDays: number;
  stepGoal: number;
  today: ActivityDay;
  history: ActivityDay[];
  insight: string;
}

export interface WaterEntry {
  id: number;
  amountMl: number;
  loggedAt: string;
}

export interface WaterDay {
  date: string;
  goalMl: number;
  totalMl: number;
  remainingMl: number;
  percent: number;
  customGoal: boolean;
  goalReason?: string;
  entries: WaterEntry[];
  guidance: string[];
}

export interface WaterHistoryDay {
  date: string;
  totalMl: number;
  goalMl: number;
}

export interface SleepEntry {
  date: string;
  sleepStart: string;
  wakeTime: string;
  bedTimeLocal: string;
  wakeTimeLocal: string;
  durationMin: number;
  quality?: number;
  restDay: boolean;
  notes?: string;
}

export interface Recovery {
  averageSleepHours7d?: number;
  sleepGoalHours: number;
  averageEffort7d?: number;
  workoutsLast7Days: number;
  status: 'GOOD' | 'MODERATE' | 'LOW';
  suggestedIntensity: string;
  tips: string[];
}
