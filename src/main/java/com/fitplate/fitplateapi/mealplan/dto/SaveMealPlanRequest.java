package com.fitplate.fitplateapi.mealplan.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaveMealPlanRequest {
    @NotBlank(message = "목표는 필수입니다")
    private String goal;

    @NotNull(message = "기간은 필수입니다")
    @Min(value = 1, message = "기간은 1일 이상이어야 합니다")
    @Max(value = 365, message = "기간은 365일 이하여야 합니다")
    private Integer durationDays;

    @NotNull(message = "AI 식단 응답은 필수입니다")
    private JsonNode aiMealPlanResponse;
}
