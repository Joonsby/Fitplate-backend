package com.fitplate.fitplateapi.mealplan.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 식단 계획 요청 DTO (Data Transfer Object)
 *
 * 📌 DTO란?
 * - 계층간(Controller ↔ Service)에서 데이터를 전달하는 객체
 * - DB 엔티티와 다르게 필요한 데이터만 포함
 * - 요청/응답 형식을 명확히 정의
 *
 * 📌 요청 흐름:
 * 클라이언트의 JSON
 * ↓ (Jackson이 자동으로 역직렬화)
 * MealPlanRequest DTO 객체
 * ↓ (@Valid로 검증)
 * Service 계층으로 전달
 *
 * 📌 Lombok 어노테이션:
 * @Getter: 모든 필드의 getter 메서드 자동 생성
 * @Builder: 빌더 패턴 사용 가능 (객체 생성 시 편리함)
 * @NoArgsConstructor: 파라미터 없는 생성자 자동 생성 (Jackson 역직렬화 시 필요)
 * @AllArgsConstructor: 모든 필드를 파라미터로 받는 생성자 자동 생성
 *
 * 📌 검증 어노테이션 (@Valid로 자동 검증):
 * @NotNull: null 불가
 * @NotBlank: 빈 문자열 불가
 * @Min/@Max: 최솟값/최댓값 제약
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
    @Min(value = 1, message = "기간은 1일 이상이어야 합니다")
    @Max(value = 365, message = "기간은 365일 이하여야 합니다")
    private Integer periodDays;
}

