// 바꿀 음식과 대안 음식 목록 응답입니다. favoriteNames 는 즐겨찾기에서 가져온 대안의 이름입니다.
package com.fitplate.fitplateapi.mealplan.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class FoodAlternativesResponse {
    private FoodItem original;
    private List<FoodItem> alternatives;
    private List<String> favoriteNames;
}
