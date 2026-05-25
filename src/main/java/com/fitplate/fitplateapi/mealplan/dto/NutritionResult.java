package com.fitplate.fitplateapi.mealplan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class NutritionResult {
    private int bmr;
    private int tdee;
    private int targetCalories;
    private int proteinGram;
    private int fatGram;
    private int carbsGram;
}
