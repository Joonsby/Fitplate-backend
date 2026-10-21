// 식단 이름 변경 요청입니다. 비우거나 null 이면 자동 이름으로 되돌립니다.
package com.fitplate.fitplateapi.mealplan.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MealPlanTitleRequest {
    @Size(max = 30, message = "식단 이름은 30자까지 쓸 수 있어요")
    private String title;
}
