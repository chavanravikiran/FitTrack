export interface DailyTargets {
  bmrKcal?: number;
  tdeeKcal?: number;
  calorieTargetKcal?: number;
  proteinMinG?: number;
  proteinMaxG?: number;
  carbsG?: number;
  fatG?: number;
  waterMl: number;
  waterTrainingDayMl: number;
  stepTarget: number;
  sleepGoalHours: number;
}

export interface RecommendationItem {
  category: string;
  icon: string;
  title: string;
  value: string;
  explanation: string;
}

export interface Recommendations {
  bmi?: number;
  bmiCategory?: string;
  targets: DailyTargets;
  items: RecommendationItem[];
  disclaimer: string;
}
