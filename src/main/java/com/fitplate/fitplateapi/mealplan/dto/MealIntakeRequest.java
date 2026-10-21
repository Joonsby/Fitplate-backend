// 끼니를 먹었는지 기록하는 요청입니다. 토글이 아니라 원하는 상태를 보내 중복 요청에도 결과가 같습니다.
package com.fitplate.fitplateapi.mealplan.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MealIntakeRequest {
    @NotNull(message = "섭취 여부는 필수입니다")
    private Boolean eaten;
}
