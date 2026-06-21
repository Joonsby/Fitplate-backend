package com.fitplate.fitplateapi.mealplan.dto;

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

    @NotNull(message = "기간은 필수입니다")
    private Integer durationDays;

    @AssertTrue(message = "식단 기간은 3일, 5일, 7일만 선택할 수 있습니다")
    public boolean isValidDurationDays() {
        return durationDays != null && (durationDays == 3 || durationDays == 5 || durationDays == 7);
    }
}

