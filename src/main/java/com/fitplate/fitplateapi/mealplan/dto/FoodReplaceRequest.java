// 식단의 음식 한 개를 대안 음식으로 바꾸는 요청입니다.
package com.fitplate.fitplateapi.mealplan.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodReplaceRequest {
    @NotNull(message = "끼니는 필수입니다")
    @Pattern(regexp = "breakfast|lunch|dinner", message = "끼니는 breakfast, lunch, dinner 중 하나여야 합니다")
    private String mealType;

    @NotNull(message = "음식 순서는 필수입니다")
    @Min(value = 0, message = "음식 순서는 0 이상이어야 합니다")
    private Integer foodIndex;

    @NotNull(message = "바꿀 음식은 필수입니다")
    private FoodItem food;
}
