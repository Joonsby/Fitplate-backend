// AI 가 돌려준 음식 대안 목록 JSON 을 받는 DTO 입니다.
package com.fitplate.fitplateapi.ai;

import com.fitplate.fitplateapi.mealplan.dto.FoodItem;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AiFoodAlternatives {
    private List<FoodItem> alternatives;
}
