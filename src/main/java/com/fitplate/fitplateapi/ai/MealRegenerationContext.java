// 한 끼를 AI 로 다시 만들 때 필요한 목표 영양과 주변 끼니 정보입니다.
package com.fitplate.fitplateapi.ai;

import com.fitplate.fitplateapi.mealplan.dto.FoodItem;
import com.fitplate.fitplateapi.user.domain.FoodPreferences;

import java.util.List;

public record MealRegenerationContext(
        String goal,
        String mealType,
        MealTarget target,
        List<FoodItem> currentFoods,
        List<FoodItem> otherMealFoods,
        FoodPreferences preferences,
        PersonalizationHints hints,
        String feedback
) {
    /** 다시 만들 끼니가 맞춰야 할 영양 합계. 기존 끼니의 합계를 그대로 쓴다. */
    public record MealTarget(int calories, double protein, double carbohydrate, double fat) {
    }
}
