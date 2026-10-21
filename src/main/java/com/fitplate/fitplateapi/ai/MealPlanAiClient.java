package com.fitplate.fitplateapi.ai;

import com.fitplate.fitplateapi.mealplan.dto.FoodItem;
import com.fitplate.fitplateapi.mealplan.dto.MealPlanRequest;
import com.fitplate.fitplateapi.mealplan.dto.MealPlanResponse;
import com.fitplate.fitplateapi.mealplan.dto.MealResponse;
import com.fitplate.fitplateapi.nutrition.dto.NutritionResult;
import com.fitplate.fitplateapi.user.domain.FoodPreferences;

import java.util.List;

public interface MealPlanAiClient {
    /** hints 는 사용 기록으로 만든 참고 정보다. feedback 은 재요청 시 이전 결과의 문제점이고 첫 요청이면 null. */
    MealPlanResponse generateMealPlan(MealPlanRequest request, NutritionResult nutritionResult,
                                      FoodPreferences preferences, PersonalizationHints hints, String feedback);

    List<FoodItem> suggestAlternatives(FoodAlternativeContext context);

    MealResponse regenerateMeal(MealRegenerationContext context);
}
