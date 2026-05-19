package com.fitplate.fitplateapi.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 식단 계획 요청 DTO
 * 사용자의 신체 정보와 목표를 받아서 맞춤형 식단을 생성하기 위한 요청 데이터
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealPlanRequest {

    /**
     * 사용자의 키 (센티미터 단위)
     * 필수값, 100 이상 300 이하
     */
    @NotNull(message = "키는 필수입니다")
    @Min(value = 100, message = "키는 100cm 이상이어야 합니다")
    @Max(value = 300, message = "키는 300cm 이하여야 합니다")
    private Integer height;

    /**
     * 사용자의 체중 (킬로그램 단위)
     * 필수값, 20 이상 300 이하
     */
    @NotNull(message = "체중은 필수입니다")
    @Min(value = 20, message = "체중은 20kg 이상이어야 합니다")
    @Max(value = 300, message = "체중은 300kg 이하여야 합니다")
    private Integer weight;

    /**
     * 사용자의 나이
     * 필수값, 10 이상 120 이하
     */
    @NotNull(message = "나이는 필수입니다")
    @Min(value = 10, message = "나이는 10 이상이어야 합니다")
    @Max(value = 120, message = "나이는 120 이하여야 합니다")
    private Integer age;

    /**
     * 사용자의 성별
     * 필수값, "MALE" 또는 "FEMALE"
     */
    @NotBlank(message = "성별은 필수입니다")
    private String gender;

    /**
     * 사용자의 체지방률 (선택사항)
     * 0 이상 100 이하
     */
    @Min(value = 0, message = "체지방률은 0% 이상이어야 합니다")
    @Max(value = 100, message = "체지방률은 100% 이하여야 합니다")
    private Double bodyFatRate;

    /**
     * 사용자의 목표
     * 필수값, 예: "WEIGHT_LOSS", "MUSCLE_GAIN", "DIET_BALANCE"
     */
    @NotBlank(message = "목표는 필수입니다")
    private String goal;

    /**
     * 식단 계획 기간 (일 단위)
     * 필수값, 1 이상 365 이하
     */
    @NotNull(message = "기간은 필수입니다")
    @Min(value = 1, message = "기간은 1일 이상이어야 합니다")
    @Max(value = 365, message = "기간은 365일 이하여야 합니다")
    private Integer periodDays;
}

