package com.fitplate.fitplateapi.mealplan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 각 식사(아침/점심/저녁)의 음식명과 영양 정보를 담는 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealResponse {

    /** 식사 이름 (쉼표로 구분된 음식명 목록) */
    private String name;

    /** 식사의 총 칼로리 (kcal) */
    private Integer calories;

    /** 단백질 함량 (그램) */
    private Double protein;

    /** 탄수화물 함량 (그램) */
    private Double carbohydrate;

    /** 지방 함량 (그램) */
    private Double fat;
}

