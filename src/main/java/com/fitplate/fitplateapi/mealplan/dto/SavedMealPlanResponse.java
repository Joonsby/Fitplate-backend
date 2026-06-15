package com.fitplate.fitplateapi.mealplan.dto;

import com.fitplate.fitplateapi.mealplan.domain.MealPlan;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SavedMealPlanResponse {

    private Long id;
    private String goal;
    private Integer durationDays;

    private Integer heightCm;
    private Integer weightKg;
    private Integer age;
    private String gender;

    private Integer targetCalories;
    private Integer bmr;
    private Integer tdee;
    private Integer proteinGram;
    private Integer carbsGram;
    private Integer fatGram;

    private String aiResponseJson;

    private LocalDateTime startedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SavedMealPlanResponse from(MealPlan mealPlan) {
        return SavedMealPlanResponse.builder()
                .id(mealPlan.getId())
                .goal(mealPlan.getGoal())
                .durationDays(mealPlan.getDurationDays())
                .heightCm(mealPlan.getHeightCm())
                .weightKg(mealPlan.getWeightKg())
                .age(mealPlan.getAge())
                .gender(mealPlan.getGender())
                .targetCalories(mealPlan.getTargetCalories())
                .bmr(mealPlan.getBmr())
                .tdee(mealPlan.getTdee())
                .proteinGram(mealPlan.getProteinGram())
                .carbsGram(mealPlan.getCarbsGram())
                .fatGram(mealPlan.getFatGram())
                .aiResponseJson(mealPlan.getAiResponseJson())
                .startedAt(mealPlan.getStartedAt())
                .expiresAt(mealPlan.getExpiresAt())
                .createdAt(mealPlan.getCreatedAt())
                .updatedAt(mealPlan.getUpdatedAt())
                .build();
    }
}