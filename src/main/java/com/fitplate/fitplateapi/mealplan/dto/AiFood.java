package com.fitplate.fitplateapi.mealplan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiFood {

    private String name;

    private String amount;

    private Integer calories;

    private Double protein;

    private Double carbohydrate;

    private Double fat;

    private String shoppingKeyword;
}