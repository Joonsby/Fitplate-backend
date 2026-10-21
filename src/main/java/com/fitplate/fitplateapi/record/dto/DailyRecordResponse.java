// 하루치 식단 기록: 지정된 식단, 끼니별 섭취 여부, 목표·계획·실제 섭취 영양 합계입니다.
package com.fitplate.fitplateapi.record.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * completionRate 는 먹은 끼니 수 / 계획 끼니 수이고, 식단이 없거나 아직 오지 않은 날은 null 이다.
 * target 은 그날 식단의 목표 섭취량, planned 는 AI 가 짜 준 식단 전체의 합계다.
 * eaten 은 먹었다고 체크한 끼니만의 합계이고, 체크가 하나도 없으면 0 이 아니라 null(기록 없음)이다.
 */
public record DailyRecordResponse(
        LocalDate date,
        RecordStatus status,
        Long mealPlanId,
        String goal,
        int plannedMeals,
        int eatenMeals,
        Double completionRate,
        NutritionTotals target,
        NutritionTotals planned,
        NutritionTotals eaten,
        List<MealRecord> meals
) {
    public record MealRecord(String mealType, String title, boolean eaten, int calories, List<String> foodNames) {
    }

    public enum RecordStatus {
        /** 이 날짜에 지정된 식단이 없다. */
        NO_PLAN,
        /** 아직 오지 않은 날짜다. */
        UPCOMING,
        /** 식단은 있지만 체크한 끼니가 없다. 섭취량 0 이 아니라 기록 없음이다. */
        NOT_RECORDED,
        PARTIAL,
        COMPLETE
    }
}
