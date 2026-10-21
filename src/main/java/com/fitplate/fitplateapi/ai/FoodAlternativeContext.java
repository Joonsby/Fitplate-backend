// 음식 한 개를 대신할 대안을 AI 에 물을 때 필요한 끼니 정보입니다.
package com.fitplate.fitplateapi.ai;

import com.fitplate.fitplateapi.mealplan.dto.FoodItem;
import com.fitplate.fitplateapi.user.domain.FoodPreferences;

import java.util.List;

public record FoodAlternativeContext(
        String goal,
        String mealType,
        List<FoodItem> otherFoods,
        FoodItem target,
        FoodPreferences preferences,
        int count,
        List<String> avoidNames
) {
}
