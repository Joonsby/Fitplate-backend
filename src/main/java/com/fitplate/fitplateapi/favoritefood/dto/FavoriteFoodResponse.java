package com.fitplate.fitplateapi.favoritefood.dto;

import com.fitplate.fitplateapi.favoritefood.domain.FavoriteFood;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class FavoriteFoodResponse {

    private Long favoriteFoodId;
    private String name;
    private String amount;

    private Integer calories;
    private BigDecimal carbohydrate;
    private BigDecimal protein;
    private BigDecimal fat;

    private String shoppingCategory;
    private String shoppingKeyword;
    private String sourceFoodId;

    private LocalDateTime createdAt;

    public static FavoriteFoodResponse from(FavoriteFood food) {
        return FavoriteFoodResponse.builder()
                .favoriteFoodId(food.getFavoriteFoodId())
                .name(food.getFoodName())
                .amount(food.getAmount())
                .calories(food.getCalories())
                .carbohydrate(food.getCarbohydrate())
                .protein(food.getProtein())
                .fat(food.getFat())
                .shoppingCategory(food.getShoppingCategory())
                .shoppingKeyword(food.getShoppingKeyword())
                .sourceFoodId(food.getSourceFoodId())
                .createdAt(food.getCreatedAt())
                .build();
    }
}