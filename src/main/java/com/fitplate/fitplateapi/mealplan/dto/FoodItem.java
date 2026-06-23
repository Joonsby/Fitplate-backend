package com.fitplate.fitplateapi.mealplan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodItem {
    private String mealType;
    private String name;
    private String amount;
    private Integer calories;
    private Double protein;
    private Double carbohydrate;
    private Double fat;
    private String shoppingKeyword;
}