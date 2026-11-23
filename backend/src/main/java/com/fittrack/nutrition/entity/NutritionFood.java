package com.fittrack.nutrition.entity;

import com.fittrack.common.entity.BaseEntity;
import com.fittrack.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** A food with nutrient values per serving. System foods are curated; users can add their own. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "nutrition_foods")
public class NutritionFood extends BaseEntity {

    public enum Category { GRAINS, PULSES, DAIRY, EGGS, MEAT, SEAFOOD, FRUITS, VEGETABLES, NUTS_SEEDS, BEVERAGES, SNACKS, PREPARED, OTHER }

    public enum DietType { VEGETARIAN, NON_VEGETARIAN, VEGAN, EGGETARIAN }

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(name = "diet_type", nullable = false, length = 15)
    private DietType dietType;

    @Column(name = "serving_qty", nullable = false, precision = 7, scale = 2)
    private BigDecimal servingQty;

    @Column(name = "serving_unit", nullable = false, length = 20)
    private String servingUnit;

    @Column(nullable = false, precision = 7, scale = 2)
    private BigDecimal calories;

    @Column(name = "protein_g", nullable = false, precision = 6, scale = 2)
    private BigDecimal proteinG;

    @Column(name = "carbs_g", nullable = false, precision = 6, scale = 2)
    private BigDecimal carbsG;

    @Column(name = "fat_g", nullable = false, precision = 6, scale = 2)
    private BigDecimal fatG;

    @Column(name = "is_system", nullable = false)
    private boolean system = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User owner;

    @Column(nullable = false)
    private boolean active = true;
}
