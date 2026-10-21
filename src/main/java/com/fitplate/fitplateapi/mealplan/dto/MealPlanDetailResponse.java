package com.fitplate.fitplateapi.mealplan.dto;

import com.fitplate.fitplateapi.mealplan.domain.MealPlan;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealPlanDetailResponse {

    private Long id;
    /** 보여줄 이름. 사용자가 정하지 않았으면 음식으로 만든 자동 이름. */
    private String title;
    /** 사용자가 직접 정한 이름이면 true. */
    private boolean titleCustomized;

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

    /** 이 식단을 따르는 날짜. null 이면 날짜 미지정. */
    private LocalDate planDate;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static MealPlanDetailResponse from(MealPlan mealPlan) {
        return MealPlanDetailResponse.builder()
                .id(mealPlan.getId())
                .title(mealPlan.displayTitle())
                .titleCustomized(mealPlan.getTitle() != null)
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
                .aiMealPlanResponse(MealPlanResponse.from(mealPlan))
                .planDate(mealPlan.getPlanDate())
                .createdAt(mealPlan.getCreatedAt())
                .updatedAt(mealPlan.getUpdatedAt())
                .build();
    }

}
