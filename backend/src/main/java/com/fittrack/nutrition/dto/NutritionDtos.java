package com.fittrack.nutrition.dto;

import com.fittrack.nutrition.entity.MealRecord.MealType;
import com.fittrack.nutrition.entity.NutritionFood.Category;
import com.fittrack.nutrition.entity.NutritionFood.DietType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class NutritionDtos {

    private NutritionDtos() {
    }

    public record FoodDto(Long id, String name, Category category, DietType dietType, BigDecimal servingQty,
                          String servingUnit, BigDecimal calories, BigDecimal proteinG, BigDecimal carbsG,
                          BigDecimal fatG, boolean custom) {
    }

    public record FoodRequest(
            @NotBlank @Size(max = 120) String name,
            @NotNull Category category,
            @NotNull DietType dietType,
            @NotNull @DecimalMin(value = "0.01") @DecimalMax("10000") @Digits(integer = 5, fraction = 2) BigDecimal servingQty,
            @NotBlank @Size(max = 20) String servingUnit,
            @NotNull @DecimalMin("0") @DecimalMax("5000") BigDecimal calories,
            @NotNull @DecimalMin("0") @DecimalMax("500") BigDecimal proteinG,
            @NotNull @DecimalMin("0") @DecimalMax("1000") BigDecimal carbsG,
            @NotNull @DecimalMin("0") @DecimalMax("500") BigDecimal fatG) {
    }

    public record AddItemRequest(@NotNull Long foodId,
                                 @NotNull @DecimalMin("0.25") @DecimalMax("20") @Digits(integer = 2, fraction = 2) BigDecimal servings) {
    }

    public record UpdateItemRequest(@NotNull @DecimalMin("0.25") @DecimalMax("20") @Digits(integer = 2, fraction = 2) BigDecimal servings) {
    }

    public record Totals(int calories, int proteinG, int carbsG, int fatG) {
        public static final Totals ZERO = new Totals(0, 0, 0, 0);
    }

    public record MealItemDto(Long id, Long foodId, String foodName, BigDecimal servings, BigDecimal servingQty,
                              String servingUnit, BigDecimal calories, BigDecimal proteinG, BigDecimal carbsG, BigDecimal fatG) {
    }

    public record MealDto(MealType mealType, List<MealItemDto> items, Totals totals) {
    }

    public record DayNutritionDto(LocalDate date, List<MealDto> meals, Totals totals, Totals targets,
                                  List<String> insights, String disclaimer) {
    }

    public record DailyTotalsDto(LocalDate date, Totals totals) {
    }

    public record SuggestionDto(FoodDto food, String reason) {
    }
}
