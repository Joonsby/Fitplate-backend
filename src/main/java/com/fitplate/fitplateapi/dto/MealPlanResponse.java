package com.fitplate.fitplateapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 생성된 전체 식단 계획의 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealPlanResponse {

    /**
     * 지정된 기간의 모든 일일 식단 정보
     * 각 요소가 하루의 식단(아침, 점심, 저녁)을 포함
     */
    private List<MealDayResponse> days;
}

