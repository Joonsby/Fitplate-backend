package com.fitplate.fitplateapi.mealplan.dto;

import com.fitplate.fitplateapi.mealplan.domain.MealPlanMeal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 하루의 식단 정보를 담는 DTO (아침/점심/저녁 끼니 포함)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealResponse {
    private String mealType;
    private String title;
    private List<FoodItem> foods;

    public static MealResponse from(MealPlanMeal meal) {
        return MealResponse.builder()
                .mealType(meal.getMealType())
                .title(meal.getTitle())
                .foods(meal.getFoods().stream().map(FoodItem::from).toList())
                .build();
    }
}

