// 식단 안의 음식 한 개 위치(끼니 + 끼니 안 순서)를 가리키는 요청입니다.
package com.fitplate.fitplateapi.mealplan.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodPositionRequest {
    @NotNull(message = "끼니는 필수입니다")
    @Pattern(regexp = "breakfast|lunch|dinner", message = "끼니는 breakfast, lunch, dinner 중 하나여야 합니다")
    private String mealType;

    @NotNull(message = "음식 순서는 필수입니다")
    @Min(value = 0, message = "음식 순서는 0 이상이어야 합니다")
    private Integer foodIndex;

    /** 대안 조회에서만 쓴다. 이번에 이미 보여준 음식 이름으로, 다시 제안하지 않는다. */
    @Size(max = 60, message = "제외할 음식은 60개까지 보낼 수 있습니다")
    private List<String> excludeNames;
}
