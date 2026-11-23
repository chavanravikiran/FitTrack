package com.fittrack.nutrition.entity;

import com.fittrack.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** A logged food. Nutrients are snapshotted so later edits to the food never rewrite history. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "meal_record_items")
public class MealRecordItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meal_record_id", nullable = false)
    private MealRecord mealRecord;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "food_id", nullable = false)
    private NutritionFood food;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal servings;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal calories;

    @Column(name = "protein_g", nullable = false, precision = 7, scale = 2)
    private BigDecimal proteinG;

    @Column(name = "carbs_g", nullable = false, precision = 7, scale = 2)
    private BigDecimal carbsG;

    @Column(name = "fat_g", nullable = false, precision = 7, scale = 2)
    private BigDecimal fatG;

    public void applyServings(BigDecimal servings) {
        this.servings = servings;
        this.calories = food.getCalories().multiply(servings).setScale(2, RoundingMode.HALF_UP);
        this.proteinG = food.getProteinG().multiply(servings).setScale(2, RoundingMode.HALF_UP);
        this.carbsG = food.getCarbsG().multiply(servings).setScale(2, RoundingMode.HALF_UP);
        this.fatG = food.getFatG().multiply(servings).setScale(2, RoundingMode.HALF_UP);
    }
}
