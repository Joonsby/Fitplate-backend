package com.fitplate.fitplateapi.mealplan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 각 식사(아침, 점심, 저녁)의 정보를 담는 DTO
 *
 * 📌 계층 구조:
 * MealPlanResponse
 *   └─ List<MealDayResponse> (각 날짜)
 *       └─ breakfast: MealResponse (이 클래스)
 *       └─ lunch: MealResponse (이 클래스)
 *       └─ dinner: MealResponse (이 클래스)
 *
 * 📌 JSON 예시:
 * {
 *   "name": "계란말이, 흰쌀밥, 미역국",
 *   "calories": 400,
 *   "protein": 25.5,
 *   "carbohydrate": 45.0,
 *   "fat": 12.0
 * }
 *
 * 📌 특징:
 * - AI(Gemini)가 생성한 한국식 식단 데이터
 * - 각 음식의 영양 정보 포함
 * - 클라이언트에 JSON으로 직렬화되어 반환됨
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealResponse {

    /**
     * 식사 이름 (음식명 리스트)
     *
     * 📌 예시:
     * "계란말이, 흰쌀밥, 미역국"
     * "불고기덮밥, 미소된장국"
     * "생선까스, 감자튀김, 샐러드"
     *
     * 📌 특징:
     * - 한국식 음식 위주
     * - 여러 음식을 포함할 수 있음
     * - 쉼표(,)로 구분
     *
     * String name: 식사를 구성하는 음식들
     */
    private String name;

    /**
     * 식사의 총 칼로리 (kcal)
     *
     * 📌 예시:
     * 아침: 300~500 kcal
     * 점심: 600~800 kcal
     * 저녁: 500~700 kcal
     *
     * 📌 사용:
     * - 하루 목표 칼로리 계산 시 사용
     * - 클라이언트에서 칼로리 추적 시 사용
     *
     * Integer calories: 한끼 음식의 칼로리
     */
    private Integer calories;

    /**
     * 단백질 함량 (그램)
     *
     * 📌 역할:
     * - 근육 유지 및 성장
     * - 포만감 유지
     *
     * 📌 예시:
     * 계란말이: 15g
     * 불고기: 30g
     *
     * 📌 목표:
     * - 근력운동: 체중 당 1.6~2.2g/kg
     *
     * Double protein: 단백질 그램
     */
    private Double protein;

    /**
     * 탄수화물 함량 (그램)
     *
     * 📌 역할:
     * - 에너지 공급 (가장 중요한 에너지원)
     * - 뇌와 신체 활동 지원
     *
     * 📌 예시:
     * 흰쌀밥 한공기: 45g
     * 식빵 한조각: 15g
     *
     * 📌 비율:
     * - 일일 칼로리의 40~65% 정도
     *
     * Double carbohydrate: 탄수화물 그램
     */
    private Double carbohydrate;

    /**
     * 지방 함량 (그램)
     *
     * 📌 역할:
     * - 호르몬 생성
     * - 영양소 흡수 촉진
     * - 에너지 저장
     *
     * 📌 예시:
     * 버터 한 큰 스푼: 12g
     * 계란 한개: 5g
     *
     * 📌 비율:
     * - 일일 칼로리의 20~35% 정도
     * - 과다 섭취 주의
     *
     * Double fat: 지방 그램
     */
    private Double fat;
}

