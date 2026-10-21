package com.fitplate.fitplateapi.mealplan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealPlanGenerateResponse {

    /** 저장된 식단 ID. 음식 바꾸기에 쓴다. */
    private Long mealPlanId;
    /** 자동으로 만든 식단 이름. */
    private String title;

    private Integer height;
    private Integer weight;
    private Integer age;
    private String gender;
    private String goal;

    private Integer targetCalories;
    private Integer bmr;
    private Integer tdee;
    private Integer proteinGram;
    private Integer carbsGram;
    private Integer fatGram;

    private MealPlanResponse aiMealPlanResponse;
}