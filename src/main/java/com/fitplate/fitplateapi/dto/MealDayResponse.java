package com.fitplate.fitplateapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 하루의 식단 정보를 담는 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealDayResponse {

    /**
     * 날짜 번호 (1부터 시작)
     */
    private Integer dayNumber;

    /**
     * 아침 식사
     */
    private MealResponse breakfast;

    /**
     * 점심 식사
     */
    private MealResponse lunch;

    /**
     * 저녁 식사
     */
    private MealResponse dinner;
}

