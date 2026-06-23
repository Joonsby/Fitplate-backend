package com.fitplate.fitplateapi.mealplan.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitplate.fitplateapi.mealplan.domain.MealPlan;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

import static com.fitplate.fitplateapi.mealplan.dto.MealPlanDetailResponse.toMealPlanResponse;

@Getter
@Builder
public class SavedMealPlanResponse {

    private Long id;
    private String goal;

    private Integer height;
    private Integer weight;
    private Integer age;
    private String gender;

    private Integer targetCalories;
    private Integer bmr;
    private Integer tdee;
    private Integer proteinGram;
    private Integer carbsGram;
    private Integer fatGram;

    private MealPlanResponse aiMealPlanResponse;

    private LocalDateTime startedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SavedMealPlanResponse from(MealPlan mealPlan, ObjectMapper objectMapper) {
        return SavedMealPlanResponse.builder()
                .id(mealPlan.getId())
                .goal(mealPlan.getGoal())
                .height(mealPlan.getHeight())
                .weight(mealPlan.getWeight())
                .age(mealPlan.getAge())
                .gender(mealPlan.getGender())
                .targetCalories(mealPlan.getTargetCalories())
                .bmr(mealPlan.getBmr())
                .tdee(mealPlan.getTdee())
                .proteinGram(mealPlan.getProteinGram())
                .carbsGram(mealPlan.getCarbsGram())
                .fatGram(mealPlan.getFatGram())
                .aiMealPlanResponse(toMealPlanResponse(mealPlan.getAiResponseJson(), objectMapper))
                .startedAt(mealPlan.getStartedAt())
                .expiresAt(mealPlan.getExpiresAt())
                .createdAt(mealPlan.getCreatedAt())
                .updatedAt(mealPlan.getUpdatedAt())
                .build();
    }
}