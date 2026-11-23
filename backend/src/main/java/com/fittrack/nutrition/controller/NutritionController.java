package com.fittrack.nutrition.controller;

import com.fittrack.common.api.ApiResponse;
import com.fittrack.common.api.PageResponse;
import com.fittrack.nutrition.dto.NutritionDtos.AddItemRequest;
import com.fittrack.nutrition.dto.NutritionDtos.DailyTotalsDto;
import com.fittrack.nutrition.dto.NutritionDtos.DayNutritionDto;
import com.fittrack.nutrition.dto.NutritionDtos.FoodDto;
import com.fittrack.nutrition.dto.NutritionDtos.FoodRequest;
import com.fittrack.nutrition.dto.NutritionDtos.SuggestionDto;
import com.fittrack.nutrition.dto.NutritionDtos.UpdateItemRequest;
import com.fittrack.nutrition.entity.MealRecord.MealType;
import com.fittrack.nutrition.entity.NutritionFood.Category;
import com.fittrack.nutrition.entity.NutritionFood.DietType;
import com.fittrack.nutrition.service.NutritionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Nutrition")
@Validated
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class NutritionController {

    private final NutritionService nutritionService;

    @Operation(summary = "Search foods (system foods + my custom foods)")
    @GetMapping("/foods")
    public ApiResponse<PageResponse<FoodDto>> foods(@RequestParam(required = false) String q,
                                                    @RequestParam(required = false) Category category,
                                                    @RequestParam(required = false) DietType diet,
                                                    @RequestParam(defaultValue = "0") @Min(0) int page,
                                                    @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.ok(nutritionService.searchFoods(q, category, diet, page, size));
    }

    @Operation(summary = "Add my own food")
    @PostMapping("/foods")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FoodDto> createFood(@Valid @RequestBody FoodRequest req) {
        return ApiResponse.ok(nutritionService.createCustomFood(req), "Food added");
    }

    @DeleteMapping("/foods/{id}")
    public ApiResponse<Void> deleteFood(@PathVariable Long id) {
        nutritionService.deleteCustomFood(id);
        return ApiResponse.message("Food removed");
    }

    @Operation(summary = "Meals for a date with totals and targets (default today)")
    @GetMapping("/me/meals")
    public ApiResponse<DayNutritionDto> day(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(nutritionService.day(date));
    }

    @PostMapping("/me/meals/{date}/{mealType}/items")
    public ApiResponse<DayNutritionDto> addItem(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                                @PathVariable MealType mealType, @Valid @RequestBody AddItemRequest req) {
        return ApiResponse.ok(nutritionService.addItem(date, mealType, req));
    }

    @PutMapping("/me/meals/items/{itemId}")
    public ApiResponse<DayNutritionDto> updateItem(@PathVariable Long itemId, @Valid @RequestBody UpdateItemRequest req) {
        return ApiResponse.ok(nutritionService.updateItem(itemId, req.servings()));
    }

    @DeleteMapping("/me/meals/items/{itemId}")
    public ApiResponse<DayNutritionDto> deleteItem(@PathVariable Long itemId) {
        return ApiResponse.ok(nutritionService.deleteItem(itemId));
    }

    @Operation(summary = "Daily calorie and macro totals for the last N days")
    @GetMapping("/me/nutrition/summary")
    public ApiResponse<List<DailyTotalsDto>> summary(@RequestParam(defaultValue = "7") int days) {
        return ApiResponse.ok(nutritionService.summary(days));
    }

    @Operation(summary = "Food suggestions for a meal based on my diet preference and allergies")
    @GetMapping("/me/nutrition/suggestions")
    public ApiResponse<List<SuggestionDto>> suggestions(@RequestParam MealType mealType) {
        return ApiResponse.ok(nutritionService.suggestions(mealType));
    }
}
