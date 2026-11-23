export type MealType = 'BREAKFAST' | 'LUNCH' | 'EVENING_SNACK' | 'DINNER';
export type FoodCategory =
  | 'GRAINS' | 'PULSES' | 'DAIRY' | 'EGGS' | 'MEAT' | 'SEAFOOD' | 'FRUITS'
  | 'VEGETABLES' | 'NUTS_SEEDS' | 'BEVERAGES' | 'SNACKS' | 'PREPARED' | 'OTHER';
export type DietType = 'VEGETARIAN' | 'NON_VEGETARIAN' | 'VEGAN' | 'EGGETARIAN';

export interface Food {
  id: number;
  name: string;
  category: FoodCategory;
  dietType: DietType;
  servingQty: number;
  servingUnit: string;
  calories: number;
  proteinG: number;
  carbsG: number;
  fatG: number;
  custom: boolean;
}

export interface FoodRequest {
  name: string;
  category: FoodCategory;
  dietType: DietType;
  servingQty: number;
  servingUnit: string;
  calories: number;
  proteinG: number;
  carbsG: number;
  fatG: number;
}

export interface Totals {
  calories: number;
  proteinG: number;
  carbsG: number;
  fatG: number;
}

export interface MealItem {
  id: number;
  foodId: number;
  foodName: string;
  servings: number;
  servingQty: number;
  servingUnit: string;
  calories: number;
  proteinG: number;
  carbsG: number;
  fatG: number;
}

export interface Meal {
  mealType: MealType;
  items: MealItem[];
  totals: Totals;
}

export interface DayNutrition {
  date: string;
  meals: Meal[];
  totals: Totals;
  targets?: Totals;
  insights: string[];
  disclaimer: string;
}

export interface DailyTotals {
  date: string;
  totals: Totals;
}

export interface Suggestion {
  food: Food;
  reason: string;
}
