import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SKIP_ERROR_TOAST, SKIP_LOADING } from '../http/http-context';
import { ApiResponse, PageResponse } from '../models/api.models';
import { DailyTotals, DayNutrition, DietType, Food, FoodCategory, FoodRequest, MealType, Suggestion } from '../models/nutrition.models';

@Injectable({ providedIn: 'root' })
export class NutritionService {
  private readonly api = environment.apiUrl;

  constructor(private readonly http: HttpClient) {}

  searchFoods(q: string, category?: FoodCategory | null, diet?: DietType | null, page = 0, size = 20): Observable<PageResponse<Food>> {
    const params: Record<string, string | number> = { q, page, size };
    if (category) params['category'] = category;
    if (diet) params['diet'] = diet;
    return this.http
      .get<ApiResponse<PageResponse<Food>>>(`${this.api}/foods`, { params, context: new HttpContext().set(SKIP_LOADING, true) })
      .pipe(map((r) => r.data));
  }

  createFood(req: FoodRequest): Observable<Food> {
    return this.http
      .post<ApiResponse<Food>>(`${this.api}/foods`, req, { context: new HttpContext().set(SKIP_ERROR_TOAST, true) })
      .pipe(map((r) => r.data));
  }

  deleteFood(id: number): Observable<void> {
    return this.http.delete<ApiResponse<void>>(`${this.api}/foods/${id}`).pipe(map(() => undefined));
  }

  day(date?: string): Observable<DayNutrition> {
    const params: Record<string, string> = date ? { date } : {};
    return this.http.get<ApiResponse<DayNutrition>>(`${this.api}/me/meals`, { params }).pipe(map((r) => r.data));
  }

  addItem(date: string, mealType: MealType, foodId: number, servings: number): Observable<DayNutrition> {
    return this.http
      .post<ApiResponse<DayNutrition>>(`${this.api}/me/meals/${date}/${mealType}/items`, { foodId, servings })
      .pipe(map((r) => r.data));
  }

  updateItem(itemId: number, servings: number): Observable<DayNutrition> {
    return this.http.put<ApiResponse<DayNutrition>>(`${this.api}/me/meals/items/${itemId}`, { servings }).pipe(map((r) => r.data));
  }

  deleteItem(itemId: number): Observable<DayNutrition> {
    return this.http.delete<ApiResponse<DayNutrition>>(`${this.api}/me/meals/items/${itemId}`).pipe(map((r) => r.data));
  }

  summary(days = 7): Observable<DailyTotals[]> {
    return this.http.get<ApiResponse<DailyTotals[]>>(`${this.api}/me/nutrition/summary`, { params: { days } }).pipe(map((r) => r.data));
  }

  suggestions(mealType: MealType): Observable<Suggestion[]> {
    return this.http
      .get<ApiResponse<Suggestion[]>>(`${this.api}/me/nutrition/suggestions`, { params: { mealType } })
      .pipe(map((r) => r.data));
  }
}
