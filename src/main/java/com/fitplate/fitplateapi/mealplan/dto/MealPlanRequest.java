package com.fitplate.fitplateapi.mealplan.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
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

    /**
     * 사용자의 키 (센티미터 단위)
     *
     * 📌 검증 규칙:
     * @NotNull: 필수값 (null 불가)
     * @Min(value = 100): 100cm 이상
     * @Max(value = 300): 300cm 이하
     *
     * 📌 실제 사용 범위:
     * - 일반 성인: 150~200cm
     * - 네모드 검증값: 100~300cm (극단적 경우 방지)
     *
     * 📌 요청 예시:
     * "height": 175
     */
    @NotNull(message = "키는 필수입니다")
    @Min(value = 100, message = "키는 100cm 이상이어야 합니다")
    @Max(value = 300, message = "키는 300cm 이하여야 합니다")
    private Integer height;

    /**
     * 사용자의 체중 (킬로그램 단위)
     *
     * 📌 검증 규칙:
     * @NotNull: 필수값
     * @Min(value = 20): 20kg 이상
     * @Max(value = 300): 300kg 이하
     *
     * 📌 실제 사용 범위:
     * - 일반 성인: 40~100kg
     * - 네모드 검증값: 20~300kg (극단적 경우 방지)
     *
     * 📌 요청 예시:
     * "weight": 70
     */
    @NotNull(message = "체중은 필수입니다")
    @Min(value = 20, message = "체중은 20kg 이상이어야 합니다")
    @Max(value = 300, message = "체중은 300kg 이하여야 합니다")
    private Integer weight;

    /**
     * 사용자의 나이 (세 단위)
     *
     * 📌 검증 규칙:
     * @NotNull: 필수값
     * @Min(value = 10): 10세 이상
     * @Max(value = 120): 120세 이하
     *
     * 📌 실제 사용 범위:
     * - 일반 성인: 15~80세
     * - 네모드 검증값: 10~120세
     *
     * 📌 요청 예시:
     * "age": 30
     */
    @NotNull(message = "나이는 필수입니다")
    @Min(value = 10, message = "나이는 10 이상이어야 합니다")
    @Max(value = 120, message = "나이는 120 이하여야 합니다")
    private Integer age;

    /**
     * 사용자의 성별
     *
     * 📌 검증 규칙:
     * @NotBlank: 필수값, 빈 문자열 불가
     *
     * 📌 허용 값:
     * - "MALE": 남성
     * - "FEMALE": 여성
     *
     * 📌 사용 처:
     * - BMR 계산 시 성별에 따라 다른 공식 사용
     *
     * 📌 요청 예시:
     * "gender": "MALE"
     */
    @NotBlank(message = "성별은 필수입니다")
    private String gender;

    /**
     * 사용자의 체지방률 (선택사항)
     *
     * 📌 특징:
     * - @NotNull이 없으므로 선택사항
     * - null이어도 검증 통과
     * - 값이 있으면 검증 규칙 적용
     *
     * 📌 검증 규칙:
     * @Min(value = 0): 0% 이상
     * @Max(value = 100): 100% 이하
     *
     * 📌 실제 사용 범위:
     * - 남성: 10~25%
     * - 여성: 20~35%
     * - 네모드 검증값: 0~100%
     *
     * 📌 요청 예시:
     * "bodyFatRate": 20.5  (선택)
     * "bodyFatRate": null  (생략 가능)
     */
    @Min(value = 0, message = "체지방률은 0% 이상이어야 합니다")
    @Max(value = 100, message = "체지방률은 100% 이하여야 합니다")
    private Double bodyFatRate;

    /**
     * 사용자의 피트니스 목표
     *
     * 📌 검증 규칙:
     * @NotBlank: 필수값, 빈 문자열 불가
     *
     * 📌 허용 목표:
     * - "WEIGHT_LOSS": 체중 감량 (식단을 저칼로리로 구성)
     * - "MUSCLE_GAIN": 근육 증가 (식단을 고단백/고칼로리로 구성)
     * - "DIET_BALANCE": 식이 균형 (유지, TDEE와 동일)
     *
     * 📌 사용 처:
     * - Service의 calculateTargetCalories()에서 목표 칼로리 결정
     * - AI 프롬프트에 포함되어 식단 생성에 영향
     *
     * 📌 요청 예시:
     * "goal": "MUSCLE_GAIN"
     */
    @NotBlank(message = "목표는 필수입니다")
    private String goal;

    /**
     * 식단 계획 기간 (일 단위)
     *
     * 📌 검증 규칙:
     * @NotNull: 필수값
     * @Min(value = 1): 1일 이상
     * @Max(value = 365): 365일 이하
     *
     * 📌 실제 사용 범위:
     * - 짧은 기간: 7일 (1주)
     * - 중간 기간: 30일 (1개월)
     * - 장기 기간: 84일 (12주), 360일 (1년)
     *
     * 📌 사용 처:
     * - AI에게 전달되어 식단 생성 기간 결정
     * - MealPlan의 expiresAt 계산 (startedAt + periodDays)
     *
     * 📌 요청 예시:
     * "periodDays": 7
     * "periodDays": 30
     */
    @NotNull(message = "기간은 필수입니다")
    @Min(value = 1, message = "기간은 1일 이상이어야 합니다")
    @Max(value = 365, message = "기간은 365일 이하여야 합니다")
    private Integer periodDays;
}

