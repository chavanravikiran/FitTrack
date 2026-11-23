import { DayStatus } from '../../core/models/workout.models';
import { FoodCategory, MealType } from '../../core/models/nutrition.models';
import { ReminderType } from '../../core/models/reminder.models';
import { VideoCategory } from '../../core/models/video.models';

export const DAY_STATUS: Record<DayStatus, { label: string; icon: string; tone: string }> = {
  SCHEDULED: { label: 'Planned', icon: 'event', tone: 'neutral' },
  IN_PROGRESS: { label: 'In progress', icon: 'play_circle', tone: 'tertiary' },
  COMPLETED: { label: 'Done', icon: 'check_circle', tone: 'primary' },
  SKIPPED: { label: 'Skipped', icon: 'do_not_disturb_on', tone: 'muted' },
  RESCHEDULED: { label: 'Moved', icon: 'event_repeat', tone: 'muted' },
  MISSED: { label: 'Missed', icon: 'event_busy', tone: 'error' },
  REST: { label: 'Rest', icon: 'self_improvement', tone: 'muted' },
};

export const MEAL_LABELS: Record<MealType, { label: string; icon: string }> = {
  BREAKFAST: { label: 'Breakfast', icon: 'free_breakfast' },
  LUNCH: { label: 'Lunch', icon: 'lunch_dining' },
  EVENING_SNACK: { label: 'Evening snack', icon: 'cookie' },
  DINNER: { label: 'Dinner', icon: 'dinner_dining' },
};

export const FOOD_CATEGORIES: { value: FoodCategory; label: string }[] = [
  { value: 'GRAINS', label: 'Grains & breakfast' },
  { value: 'PULSES', label: 'Dal & legumes' },
  { value: 'DAIRY', label: 'Dairy' },
  { value: 'EGGS', label: 'Eggs' },
  { value: 'MEAT', label: 'Meat' },
  { value: 'SEAFOOD', label: 'Fish & seafood' },
  { value: 'FRUITS', label: 'Fruits' },
  { value: 'VEGETABLES', label: 'Vegetables' },
  { value: 'NUTS_SEEDS', label: 'Nuts & seeds' },
  { value: 'SNACKS', label: 'Snacks' },
  { value: 'BEVERAGES', label: 'Drinks' },
  { value: 'PREPARED', label: 'Prepared dishes' },
  { value: 'OTHER', label: 'Other' },
];

export const VIDEO_CATEGORIES: { value: VideoCategory; label: string; icon: string }[] = [
  { value: 'WARM_UP', label: 'Warm-up', icon: 'local_fire_department' },
  { value: 'CHEST', label: 'Chest', icon: 'fitness_center' },
  { value: 'BACK', label: 'Back', icon: 'accessibility_new' },
  { value: 'LEGS', label: 'Legs', icon: 'directions_run' },
  { value: 'SHOULDERS', label: 'Shoulders', icon: 'sports_gymnastics' },
  { value: 'ARMS', label: 'Arms', icon: 'sports_martial_arts' },
  { value: 'ABS', label: 'Abs & core', icon: 'self_improvement' },
  { value: 'CARDIO', label: 'Cardio', icon: 'favorite' },
  { value: 'STRETCHING', label: 'Stretching', icon: 'spa' },
];

export const REMINDER_TYPES: { value: ReminderType; label: string; icon: string; defaultTitle: string; defaultTime: string }[] = [
  { value: 'WAKE_UP', label: 'Wake up', icon: 'alarm', defaultTitle: 'Rise and shine', defaultTime: '06:30' },
  { value: 'WATER', label: 'Drink water', icon: 'water_drop', defaultTitle: 'Drink a glass of water', defaultTime: '09:00' },
  { value: 'WORKOUT', label: 'Workout', icon: 'fitness_center', defaultTitle: 'Workout time', defaultTime: '18:00' },
  { value: 'WALK', label: 'Walk', icon: 'directions_walk', defaultTitle: 'Time for a walk', defaultTime: '17:00' },
  { value: 'MEAL', label: 'Meal', icon: 'restaurant', defaultTitle: 'Meal time - log your food', defaultTime: '13:00' },
  { value: 'WEIGHT', label: 'Weigh-in', icon: 'monitor_weight', defaultTitle: 'Weekly weigh-in', defaultTime: '07:00' },
  { value: 'SLEEP', label: 'Bedtime', icon: 'bedtime', defaultTitle: 'Wind down for bed', defaultTime: '22:30' },
];

export function formatDuration(minutes: number): string {
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return h ? `${h}h ${m.toString().padStart(2, '0')}m` : `${m}m`;
}

export function shortDay(iso: string): string {
  const [y, mo, d] = iso.split('-').map(Number);
  return new Date(y, mo - 1, d).toLocaleDateString(undefined, { weekday: 'short' });
}

export function shortDate(iso: string): string {
  const [y, mo, d] = iso.split('-').map(Number);
  return new Date(y, mo - 1, d).toLocaleDateString(undefined, { day: 'numeric', month: 'short' });
}
