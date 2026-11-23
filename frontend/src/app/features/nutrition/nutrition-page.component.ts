import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { DailyTotals, DayNutrition, Meal, MealItem, MealType, Suggestion } from '../../core/models/nutrition.models';
import { NotifyService } from '../../core/services/notify.service';
import { NutritionService } from '../../core/services/nutrition.service';
import { BarDatum } from '../../shared/components/bar-chart.component';
import { MEAL_LABELS, shortDay } from '../../shared/constants/labels';
import { fromIsoDate, toIsoDate } from '../../shared/forms/validators';
import { AddFoodDialogComponent, AddFoodDialogData, AddFoodResult } from './add-food-dialog.component';

@Component({
  selector: 'ft-nutrition-page',
  standalone: false,
  templateUrl: './nutrition-page.component.html',
  styleUrl: './nutrition-page.component.scss',
})
export class NutritionPageComponent implements OnInit {
  date = new Date();
  readonly maxDate = (() => { const d = new Date(); d.setDate(d.getDate() + 1); return d; })();
  day: DayNutrition | null = null;
  chart: BarDatum[] = [];
  suggestions: Suggestion[] = [];
  suggestionMeal: MealType = 'BREAKFAST';
  readonly meals = MEAL_LABELS;
  readonly mealTypes: MealType[] = ['BREAKFAST', 'LUNCH', 'EVENING_SNACK', 'DINNER'];

  constructor(
    private readonly nutrition: NutritionService,
    private readonly dialog: MatDialog,
    private readonly notify: NotifyService,
  ) {}

  get iso(): string {
    return toIsoDate(this.date)!;
  }

  ngOnInit(): void {
    const h = new Date().getHours();
    this.suggestionMeal = h < 11 ? 'BREAKFAST' : h < 16 ? 'LUNCH' : h < 19 ? 'EVENING_SNACK' : 'DINNER';
    this.load();
    this.loadSuggestions();
  }

  shift(days: number): void {
    const d = new Date(this.date);
    d.setDate(d.getDate() + days);
    if (d > this.maxDate) return;
    this.date = d;
    this.load();
  }

  onDate(d: Date | null): void {
    if (d) {
      this.date = d;
      this.load();
    }
  }

  pct(value: number, target?: number): number {
    return target ? Math.min(100, Math.round((value / target) * 100)) : 0;
  }

  add(meal: MealType, preselectFoodId?: number): void {
    this.dialog.open<AddFoodDialogComponent, AddFoodDialogData, AddFoodResult>(AddFoodDialogComponent, {
      data: { mealLabel: this.meals[meal].label, foodId: preselectFoodId }, width: '560px', maxWidth: '95vw',
    }).afterClosed().subscribe((res) => {
      if (!res) return;
      this.nutrition.addItem(this.iso, meal, res.food.id, res.servings).subscribe((d) => {
        this.day = d;
        this.notify.success(`${res.food.name} added to ${this.meals[meal].label.toLowerCase()}`);
        this.loadSummary();
      });
    });
  }

  addSuggestion(s: Suggestion): void {
    this.nutrition.addItem(this.iso, this.suggestionMeal, s.food.id, 1).subscribe((d) => {
      this.day = d;
      this.notify.success(`${s.food.name} added`);
      this.loadSummary();
    });
  }

  changeServings(item: MealItem, delta: number): void {
    const next = Math.round((Number(item.servings) + delta) * 4) / 4;
    if (next < 0.25 || next > 20) return;
    this.nutrition.updateItem(item.id, next).subscribe((d) => {
      this.day = d;
      this.loadSummary();
    });
  }

  remove(item: MealItem): void {
    this.nutrition.deleteItem(item.id).subscribe((d) => {
      this.day = d;
      this.loadSummary();
    });
  }

  mealOf(type: MealType): Meal | undefined {
    return this.day?.meals.find((m) => m.mealType === type);
  }

  loadSuggestions(): void {
    this.nutrition.suggestions(this.suggestionMeal).subscribe((s) => (this.suggestions = s));
  }

  private load(): void {
    this.nutrition.day(this.iso).subscribe((d) => (this.day = d));
    this.loadSummary();
  }

  private loadSummary(): void {
    this.nutrition.summary(7).subscribe((rows: DailyTotals[]) => {
      const target = this.day?.targets?.calories;
      this.chart = rows.map((r) => ({ label: shortDay(r.date), value: r.totals.calories, goal: target,
        tooltip: `${fromIsoDate(r.date)?.toLocaleDateString()}: ${r.totals.calories} kcal · P ${r.totals.proteinG} g` }));
    });
  }
}
