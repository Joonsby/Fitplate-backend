package com.fitplate.fitplateapi.mealplan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 하루의 식단 정보를 담는 DTO (아침/점심/저녁 끼니 포함)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealDayResponse {

    /** 날짜 번호 (1부터 시작) */
    private Integer dayNumber;

    /** 아침 식사 정보 */
    private MealResponse breakfast;

    /** 점심 식사 정보 */
    private MealResponse lunch;

    /** 저녁 식사 정보 */
    private MealResponse dinner;
}

