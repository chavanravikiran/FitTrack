import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormControl, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { debounceTime, distinctUntilChanged, finalize, startWith, switchMap } from 'rxjs';
import { DietType, Food, FoodCategory } from '../../core/models/nutrition.models';
import { NutritionService } from '../../core/services/nutrition.service';
import { apiErrorMessage } from '../../core/utils/api-error';
import { FOOD_CATEGORIES } from '../../shared/constants/labels';

export interface AddFoodDialogData {
  mealLabel: string;
  foodId?: number;
}

export interface AddFoodResult {
  food: Food;
  servings: number;
}

@Component({
  selector: 'ft-add-food-dialog',
  standalone: false,
  template: `
    <h2 mat-dialog-title>Add to {{ data.mealLabel.toLowerCase() }}</h2>
    <mat-dialog-content>
      @if (!creating) {
        @if (!selected) {
          <div class="search">
            <mat-form-field appearance="outline" class="grow" subscriptSizing="dynamic">
              <mat-label>Search foods</mat-label>
              <mat-icon matPrefix>search</mat-icon>
              <input matInput [formControl]="query" placeholder="roti, dal, paneer, egg…" cdkFocusInitial />
            </mat-form-field>
            <mat-form-field appearance="outline" subscriptSizing="dynamic">
              <mat-label>Category</mat-label>
              <mat-select [formControl]="category">
                <mat-option [value]="null">All</mat-option>
                @for (c of categories; track c.value) { <mat-option [value]="c.value">{{ c.label }}</mat-option> }
              </mat-select>
            </mat-form-field>
          </div>
          @if (loading) { <mat-progress-bar mode="indeterminate" /> }
          <mat-nav-list class="results">
            @for (f of results; track f.id) {
              <a mat-list-item (click)="select(f)">
                <span matListItemTitle>{{ f.name }} @if (f.custom) { <span class="tag">mine</span> }</span>
                <span matListItemLine class="ft-muted">{{ f.servingQty }} {{ f.servingUnit }} · {{ f.calories | number: '1.0-0' }} kcal · P {{ f.proteinG }} g</span>
              </a>
            }
          </mat-nav-list>
          @if (!loading && !results.length) { <p class="ft-muted">No foods found. You can add your own.</p> }
          <button mat-button (click)="creating = true"><mat-icon>add</mat-icon> Create a custom food</button>
        } @else {
          <div class="chosen">
            <strong>{{ selected.name }}</strong>
            <span class="ft-muted">1 serving = {{ selected.servingQty }} {{ selected.servingUnit }}</span>
          </div>
          <mat-form-field appearance="outline" class="ft-full-width">
            <mat-label>Servings</mat-label>
            <input matInput type="number" min="0.25" max="20" step="0.25" [formControl]="servings" />
            <mat-error>Between 0.25 and 20</mat-error>
          </mat-form-field>
          <div class="preview">
            <span><strong>{{ calc(selected.calories) }}</strong> kcal</span>
            <span>P <strong>{{ calc(selected.proteinG) }}</strong> g</span>
            <span>C <strong>{{ calc(selected.carbsG) }}</strong> g</span>
            <span>F <strong>{{ calc(selected.fatG) }}</strong> g</span>
          </div>
          <button mat-button (click)="selected = null"><mat-icon>arrow_back</mat-icon> Choose another food</button>
        }
      } @else {
        @if (error) { <p class="error" role="alert">{{ error }}</p> }
        <form [formGroup]="custom" class="ft-form" (ngSubmit)="saveCustom()" id="custom-food">
          <mat-form-field appearance="outline"><mat-label>Name</mat-label><input matInput formControlName="name" maxlength="120" /></mat-form-field>
          <div class="ft-form-grid">
            <mat-form-field appearance="outline">
              <mat-label>Category</mat-label>
              <mat-select formControlName="category">@for (c of categories; track c.value) { <mat-option [value]="c.value">{{ c.label }}</mat-option> }</mat-select>
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Diet</mat-label>
              <mat-select formControlName="dietType">
                <mat-option value="VEGAN">Vegan</mat-option><mat-option value="VEGETARIAN">Vegetarian</mat-option>
                <mat-option value="EGGETARIAN">Contains egg</mat-option><mat-option value="NON_VEGETARIAN">Non-vegetarian</mat-option>
              </mat-select>
            </mat-form-field>
            <mat-form-field appearance="outline"><mat-label>Serving size</mat-label><input matInput type="number" formControlName="servingQty" min="0.01" /></mat-form-field>
            <mat-form-field appearance="outline"><mat-label>Unit</mat-label><input matInput formControlName="servingUnit" placeholder="g, piece, bowl" /></mat-form-field>
            <mat-form-field appearance="outline"><mat-label>Calories (kcal)</mat-label><input matInput type="number" formControlName="calories" min="0" /></mat-form-field>
            <mat-form-field appearance="outline"><mat-label>Protein (g)</mat-label><input matInput type="number" formControlName="proteinG" min="0" /></mat-form-field>
            <mat-form-field appearance="outline"><mat-label>Carbs (g)</mat-label><input matInput type="number" formControlName="carbsG" min="0" /></mat-form-field>
            <mat-form-field appearance="outline"><mat-label>Fat (g)</mat-label><input matInput type="number" formControlName="fatG" min="0" /></mat-form-field>
          </div>
        </form>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancel</button>
      @if (creating) {
        <button mat-button (click)="creating = false">Back</button>
        <button mat-flat-button type="submit" form="custom-food" [disabled]="custom.invalid || saving">Save food</button>
      } @else {
        <button mat-flat-button (click)="add()" [disabled]="!selected || servings.invalid">Add</button>
      }
    </mat-dialog-actions>
  `,
  styles: `
    .search { display: flex; gap: 8px; flex-wrap: wrap; }
    .grow { flex: 1; min-width: 220px; }
    .results { max-height: 320px; overflow: auto; }
    .tag { font: var(--mat-sys-label-small); padding: 1px 6px; border-radius: 999px; background: var(--mat-sys-tertiary-container); }
    .chosen { display: flex; flex-direction: column; margin-bottom: 12px; }
    .preview { display: flex; gap: 16px; flex-wrap: wrap; margin-bottom: 8px; }
    .error { color: var(--mat-sys-error); }
  `,
})
export class AddFoodDialogComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  readonly data = inject<AddFoodDialogData>(MAT_DIALOG_DATA);
  readonly categories = FOOD_CATEGORIES;
  readonly query = new FormControl('', { nonNullable: true });
  readonly category = new FormControl<FoodCategory | null>(null);
  readonly servings = new FormControl<number>(1, { nonNullable: true, validators: [Validators.required, Validators.min(0.25), Validators.max(20)] });
  readonly custom = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
    category: ['PREPARED' as FoodCategory, Validators.required],
    dietType: ['VEGETARIAN' as DietType, Validators.required],
    servingQty: [1, [Validators.required, Validators.min(0.01)]],
    servingUnit: ['serving', Validators.required],
    calories: [0, [Validators.required, Validators.min(0), Validators.max(5000)]],
    proteinG: [0, [Validators.required, Validators.min(0), Validators.max(500)]],
    carbsG: [0, [Validators.required, Validators.min(0), Validators.max(1000)]],
    fatG: [0, [Validators.required, Validators.min(0), Validators.max(500)]],
  });

  results: Food[] = [];
  selected: Food | null = null;
  loading = false;
  creating = false;
  saving = false;
  error: string | null = null;

  constructor(
    private readonly nutrition: NutritionService,
    private readonly ref: MatDialogRef<AddFoodDialogComponent, AddFoodResult>,
  ) {}

  ngOnInit(): void {
    this.query.valueChanges.pipe(
      startWith(''),
      debounceTime(250),
      distinctUntilChanged(),
      switchMap((q) => {
        this.loading = true;
        return this.nutrition.searchFoods(q.trim(), this.category.value, null, 0, 30).pipe(finalize(() => (this.loading = false)));
      }),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe((p) => (this.results = p.content));
    this.category.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.nutrition.searchFoods(this.query.value.trim(), this.category.value, null, 0, 30).subscribe((p) => (this.results = p.content));
    });
  }

  select(f: Food): void {
    this.selected = f;
    this.servings.setValue(1);
  }

  calc(v: number): number {
    return Math.round(v * (this.servings.value || 0));
  }

  add(): void {
    if (this.selected && this.servings.valid) {
      this.ref.close({ food: this.selected, servings: this.servings.value });
    }
  }

  saveCustom(): void {
    if (this.custom.invalid) return;
    this.saving = true;
    const v = this.custom.getRawValue();
    this.nutrition.createFood({ ...v, name: v.name.trim(), servingUnit: v.servingUnit.trim() })
      .pipe(finalize(() => (this.saving = false)))
      .subscribe({
        next: (food) => {
          this.creating = false;
          this.select(food);
        },
        error: (err) => (this.error = apiErrorMessage(err)),
      });
  }
}
