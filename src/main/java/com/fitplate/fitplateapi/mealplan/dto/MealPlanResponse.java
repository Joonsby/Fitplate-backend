package com.fitplate.fitplateapi.mealplan.dto;

import com.fitplate.fitplateapi.mealplan.domain.MealPlan;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 생성된 전체 식단 계획의 응답 DTO (Gemini AI → Service → Controller)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealPlanResponse {
    private List<MealResponse> meals;

    public static MealPlanResponse from(MealPlan mealPlan) {
        return MealPlanResponse.builder()
                .meals(mealPlan.getMeals().stream().map(MealResponse::from).toList())
                .build();
    }
}

