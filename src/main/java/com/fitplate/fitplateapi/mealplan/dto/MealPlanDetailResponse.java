package com.fitplate.fitplateapi.mealplan.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitplate.fitplateapi.mealplan.domain.MealPlan;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealPlanDetailResponse {

    private Long id;

    private String goal;
    private Integer durationDays;

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

    public static MealPlanDetailResponse from(
            MealPlan mealPlan,
            ObjectMapper objectMapper
    ) {
        return MealPlanDetailResponse.builder()
                .id(mealPlan.getId())
                .goal(mealPlan.getGoal())
                .durationDays(mealPlan.getDurationDays())
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
                .aiMealPlanResponse(toMealPlanResponse(
                        mealPlan.getAiResponseJson(),
                        objectMapper
                ))
                .startedAt(mealPlan.getStartedAt())
                .expiresAt(mealPlan.getExpiresAt())
                .createdAt(mealPlan.getCreatedAt())
                .updatedAt(mealPlan.getUpdatedAt())
                .build();
    }

    public static MealPlanResponse toMealPlanResponse(
            String aiResponseJson,
            ObjectMapper objectMapper
    ) {
        try {
            return objectMapper.readValue(aiResponseJson, MealPlanResponse.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("AI 식단 JSON 파싱에 실패했습니다.", e);
        }
    }
}