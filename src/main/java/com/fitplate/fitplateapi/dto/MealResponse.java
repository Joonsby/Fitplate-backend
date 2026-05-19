package com.fitplate.fitplateapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 각 식사(아침, 점심, 저녁)의 정보를 담는 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealResponse {

    /**
     * 식사 이름 (예: "계란말이, 흰쌀밥, 미역국")
     */
    private String name;

    /**
     * 식사의 칼로리
     */
    private Integer calories;

    /**
     * 단백질 (그램)
     */
    private Double protein;

    /**
     * 탄수화물 (그램)
     */
    private Double carbohydrate;

    /**
     * 지방 (그램)
     */
    private Double fat;
}

