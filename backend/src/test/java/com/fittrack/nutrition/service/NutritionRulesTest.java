package com.fittrack.nutrition.service;

import com.fittrack.nutrition.entity.NutritionFood.DietType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NutritionRulesTest {

    @Test
    void dietCompatibility() {
        assertThat(NutritionService.compatible(DietType.VEGAN)).containsExactly(DietType.VEGAN);
        assertThat(NutritionService.compatible(DietType.VEGETARIAN)).containsExactlyInAnyOrder(DietType.VEGAN, DietType.VEGETARIAN);
        assertThat(NutritionService.compatible(DietType.EGGETARIAN)).doesNotContain(DietType.NON_VEGETARIAN);
        assertThat(NutritionService.compatible(DietType.NON_VEGETARIAN)).hasSize(4);
    }

    @Test
    void allergiesMapToFoodKeywords() {
        assertThat(NutritionService.blockedKeywords("Peanuts, lactose intolerant"))
                .contains("peanut", "paneer", "milk", "curd");
        assertThat(NutritionService.blockedKeywords(null)).isEmpty();
        assertThat(NutritionService.blockedKeywords("none")).isEmpty();
    }
}
