package com.fitplate.fitplateapi.mealplan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 생성된 전체 식단 계획의 응답 DTO (Gemini AI → Service → Controller)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealPlanResponse {

    /**
     * 지정된 기간의 일일 식단 리스트 (각 요소가 하루의 아침/점심/저녁)
     */
    private List<MealDayResponse> days;
}

