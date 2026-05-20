package com.fitplate.fitplateapi.mealplan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 하루의 식단 정보를 담는 DTO
 *
 * 📌 계층 구조:
 * MealPlanResponse (전체 식단)
 *   └─ List<MealDayResponse> (각 날짜)
 *       └─ breakfast, lunch, dinner: MealResponse (각 끼니)
 *
 * 📌 JSON 예시:
 * {
 *   "dayNumber": 1,
 *   "breakfast": {
 *     "name": "계란말이, 흰쌀밥, 미역국",
 *     "calories": 400,
 *     "protein": 25.5,
 *     "carbohydrate": 45.0,
 *     "fat": 12.0
 *   },
 *   "lunch": {
 *     "name": "불고기덮밥, 미소된장국",
 *     "calories": 650,
 *     "protein": 35.0,
 *     "carbohydrate": 70.0,
 *     "fat": 20.0
 *   },
 *   "dinner": {
 *     "name": "생선까스, 샐러드",
 *     "calories": 550,
 *     "protein": 42.0,
 *     "carbohydrate": 45.0,
 *     "fat": 15.0
 *   }
 * }
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealDayResponse {

    /**
     * 날짜 번호 (1부터 시작)
     *
     * 📌 의미:
     * 1 = 첫 번째 날
     * 2 = 두 번째 날
     * ...
     * 7 = 일주일 식단이면 마지막 날
     */
    private Integer dayNumber;

    /**
     * 아침 식사 정보
     *
     * 📌 MealResponse 필드:
     * - name: 음식명
     * - calories: 칼로리
     * - protein: 단백질(g)
     * - carbohydrate: 탄수화물(g)
     * - fat: 지방(g)
     */
    private MealResponse breakfast;

    /**
     * 점심 식사 정보
     *
     * 📌 MealResponse 필드:
     * - name: 음식명
     * - calories: 칼로리
     * - protein: 단백질(g)
     * - carbohydrate: 탄수화물(g)
     * - fat: 지방(g)
     */
    private MealResponse lunch;

    /**
     * 저녁 식사 정보
     *
     * 📌 MealResponse 필드:
     * - name: 음식명
     * - calories: 칼로리
     * - protein: 단백질(g)
     * - carbohydrate: 탄수화물(g)
     * - fat: 지방(g)
     */
    private MealResponse dinner;
}

