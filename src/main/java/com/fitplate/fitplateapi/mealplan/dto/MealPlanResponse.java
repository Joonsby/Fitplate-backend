package com.fitplate.fitplateapi.mealplan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 생성된 전체 식단 계획의 응답 DTO
 *
 * 📌 데이터 흐름:
 * Controller ← MealPlanResponse ← Service ← Gemini AI
 *
 * 📌 JSON 응답 예시:
 * {
 *   "days": [
 *     {
 *       "dayNumber": 1,
 *       "breakfast": {
 *         "name": "계란말이, 흰쌀밥, 미역국",
 *         "calories": 400,
 *         "protein": 25.5,
 *         "carbohydrate": 45.0,
 *         "fat": 12.0
 *       },
 *       "lunch": {...},
 *       "dinner": {...}
 *     },
 *     ...
 *   ]
 * }
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealPlanResponse {

    /**
     * 지정된 기간의 모든 일일 식단 정보
     *
     * 📌 구조:
     * - 각 요소는 MealDayResponse (하루의 식단)
     * - MealDayResponse는 breakfast, lunch, dinner 포함
     * - 각 끼니는 MealResponse (음식명, 칼로리, 영양소)
     *
     * 📌 예시:
     * periodDays=7이면:
     * days[0] = 1일차 식단 (아침, 점심, 저녁)
     * days[1] = 2일차 식단 (아침, 점심, 저녁)
     * ...
     * days[6] = 7일차 식단 (아침, 점심, 저녁)
     *
     * List<MealDayResponse> days: 일일 식단 리스트
     */
    private List<MealDayResponse> days;
}

