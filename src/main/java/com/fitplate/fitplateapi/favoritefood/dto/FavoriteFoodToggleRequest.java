package com.fitplate.fitplateapi.favoritefood.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class FavoriteFoodToggleRequest {

    private String name;
    private String amount;

    private Integer calories;
    private BigDecimal carbohydrate;
    private BigDecimal protein;
    private BigDecimal fat;

    private String shoppingCategory;
    private String shoppingKeyword;
    private String sourceFoodId;
}