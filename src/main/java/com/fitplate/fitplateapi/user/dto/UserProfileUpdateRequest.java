// 내 정보 화면에서 신체 정보와 음식 취향을 저장할 때 받는 요청입니다.
package com.fitplate.fitplateapi.user.dto;

import com.fitplate.fitplateapi.user.domain.FoodCategory;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileUpdateRequest {
    @NotNull(message = "키는 필수입니다")
    @Min(value = 100, message = "키는 100cm 이상이어야 합니다")
    @Max(value = 300, message = "키는 300cm 이하여야 합니다")
    private Integer height;

    @NotNull(message = "체중은 필수입니다")
    @Min(value = 20, message = "체중은 20kg 이상이어야 합니다")
    @Max(value = 300, message = "체중은 300kg 이하여야 합니다")
    private Integer weight;

    @DecimalMin(value = "0.0", message = "체지방률은 0 이상이어야 합니다")
    @DecimalMax(value = "70.0", message = "체지방률은 70 이하여야 합니다")
    private Double bodyFatRate;

    @NotNull(message = "나이는 필수입니다")
    @Min(value = 10, message = "나이는 10 이상이어야 합니다")
    @Max(value = 120, message = "나이는 120 이하여야 합니다")
    private Integer age;

    @NotBlank(message = "성별은 필수입니다")
    private String gender;

    /** null 이면 기존 값을 유지하고, 빈 배열이면 모두 해제한다. */
    private List<FoodCategory> preferredFoodCategories;

    /** null 이면 기존 값을 유지하고, 빈 배열이면 모두 해제한다. */
    private List<FoodCategory> excludedFoodCategories;

    /** 알레르기 분류. null 이면 기존 값을 유지하고, 빈 배열이면 모두 해제한다. */
    private List<FoodCategory> allergyFoodCategories;
}
