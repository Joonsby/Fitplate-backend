package com.fitplate.fitplateapi.ai;

import com.fitplate.fitplateapi.mealplan.dto.MealPlanRequest;
import com.fitplate.fitplateapi.mealplan.dto.MealPlanResponse;
import com.fitplate.fitplateapi.nutrition.dto.NutritionResult;

public interface MealPlanAiClient {
    MealPlanResponse generateMealPlan(MealPlanRequest request, NutritionResult nutritionResult);
}