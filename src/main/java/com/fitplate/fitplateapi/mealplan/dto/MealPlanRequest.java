package com.fitplate.fitplateapi.mealplan.dto;

import com.fitplate.fitplateapi.user.domain.FoodCategory;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 식단 계획 요청 DTO. 클라이언트 JSON을 받아 @Valid로 검증 후 Service로 전달.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealPlanRequest {
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

    @NotBlank(message = "목표는 필수입니다")
    private String goal;

    /** 선호 음식 분류. null 이면 저장된 값을 그대로 쓴다. */
    private List<FoodCategory> preferredFoodCategories;

    /** 제외 음식 분류. null 이면 저장된 값을 그대로 쓴다. */
    private List<FoodCategory> excludedFoodCategories;

    /** 알레르기 분류. null 이면 저장된 값을 그대로 쓴다. */
    private List<FoodCategory> allergyFoodCategories;

    /** 만든 식단을 바로 지정할 날짜. null 이면 날짜 없이 저장한다. */
    private LocalDate planDate;

    /** planDate 에 다른 식단이 있으면 그 식단의 지정을 풀고 새 식단으로 바꾼다. */
    private boolean replaceDate;
}

