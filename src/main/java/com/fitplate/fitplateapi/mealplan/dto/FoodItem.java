package com.fitplate.fitplateapi.mealplan.dto;

import com.fitplate.fitplateapi.mealplan.domain.MealPlanFood;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodItem {
    private String name;
    private String amount;
    private Integer calories;
    private Double protein;
    private Double carbohydrate;
    private Double fat;
    private String shoppingKeyword;

    public static FoodItem from(MealPlanFood food) {
        return FoodItem.builder()
                .name(food.getName())
                .amount(food.getAmount())
                .calories(food.getCalories())
                .protein(food.getProtein().doubleValue())
                .carbohydrate(food.getCarbohydrate().doubleValue())
                .fat(food.getFat().doubleValue())
                .shoppingKeyword(food.getShoppingKeyword())
                .build();
    }
}
