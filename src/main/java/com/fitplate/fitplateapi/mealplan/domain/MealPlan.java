package com.fitplate.fitplateapi.mealplan.domain;

import com.fitplate.fitplateapi.user.domain.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 식단 계획 Entity (meal_plans 테이블 매핑).
 */
@Entity
@Table(name = "meal_plans")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MealPlan {

    /** 식단 ID (PK, 자동 증가). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "meal_plan_id")
    private Long id;

    /** 이 식단이 속한 사용자 (다대일). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 사용자의 목표 (예: WEIGHT_LOSS, MUSCLE_GAIN). */
    @Column(nullable = false, length = 20)
    private String goal;

    /** 식단 계획 기간 (일). */
    @Column(nullable = false)
    private Integer durationDays;


    /** 사용자의 키 (cm). */
    @Column(nullable = false)
    private Integer height;

    /** 사용자의 체중 (kg). */
    @Column(nullable = false)
    private Integer weight;

    /** 사용자의 나이 (세). */
    @Column(nullable = false)
    private Integer age;

    /** 사용자의 성별 (예: MALE, FEMALE). */
    @Column(nullable = false, length = 10)
    private String gender;

    /** 목표에 따라 조정된 목표 칼로리 (kcal). */
    @Column(nullable = false)
    private Integer targetCalories;

    /** 기초대사량 BMR (kcal). */
    @Column(nullable = false)
    private Integer bmr;

    /** 일일 총 소비 칼로리 TDEE (kcal). */
    @Column(nullable = false)
    private Integer tdee;

    /** 하루 단백질 섭취량 (g). */
    @Column(nullable = false)
    private Integer proteinGram;

    /** 하루 탄수화물 섭취량 (g). */
    @Column(nullable = false)
    private Integer carbsGram;

    /** 하루 지방 섭취량 (g). */
    @Column(nullable = false)
    private Integer fatGram;

    /** AI가 생성한 식단 데이터 (JSON 문자열). */
    @Lob
    @Column(nullable = false, columnDefinition = "JSON")
    private String aiResponseJson;

    @Column(nullable = false, length = 64)
    private String aiResponseHash;

    /** 식단 시작 일시. */
    @Column(nullable = false)
    private LocalDateTime startedAt;

    /** 식단 종료 일시. */
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    /** 생성 시간 (자동 설정, 이후 불변). */
    @CreationTimestamp
    private LocalDateTime createdAt;

    /** 수정 시간 (수정 시마다 자동 갱신). */
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /** 사용자의 BMI. */
    @Column(nullable = false)
    private BigDecimal bmi;

    /** 체지방률 (%). */
    @Column
    private BigDecimal bodyFatRate;


    /** Service에서 식단 저장 시 모든 필드를 받아 초기화. */
    @Builder
    public MealPlan(
            User user,
            String goal,
            Integer durationDays,
            Integer height,
            Integer weight,
            Integer age,
            String gender,
            BigDecimal bmi,
            Integer targetCalories,
            Integer bmr,
            Integer tdee,
            Integer proteinGram,
            Integer carbsGram,
            Integer fatGram,
            BigDecimal bodyFatRate,
            String aiResponseJson,
            String aiResponseHash,
            LocalDateTime startedAt,
            LocalDateTime expiresAt
    ) {
        this.user = user;
        this.goal = goal;
        this.durationDays = durationDays;
        this.height = height;
        this.weight = weight;
        this.age = age;
        this.gender = gender;
        this.bmi = bmi;
        this.targetCalories = targetCalories;
        this.bmr = bmr;
        this.tdee = tdee;
        this.proteinGram = proteinGram;
        this.carbsGram = carbsGram;
        this.fatGram = fatGram;
        this.bodyFatRate = bodyFatRate;
        this.aiResponseJson = aiResponseJson;
        this.aiResponseHash = aiResponseHash;
        this.startedAt = startedAt;
        this.expiresAt = expiresAt;
    }
}