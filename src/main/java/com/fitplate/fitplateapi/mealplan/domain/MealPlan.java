package com.fitplate.fitplateapi.mealplan.domain;

import com.fitplate.fitplateapi.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 식단 계획 Entity 클래스
 *
 * 📌 Entity 란?
 * - DB의 테이블과 대응되는 Java 클래스
 * - 각 필드가 테이블의 컬럼이 됨
 * - JPA(Java Persistence API)가 자동으로 DB 작업 처리
 *
 * 📌 데이터 흐름:
 * Java Object (MealPlan) ↔ JPA ↔ DB (meal_plans 테이블)
 *
 * 📌 주요 개념:
 * @Entity: JPA가 관리하는 엔티티임을 표시
 * @Table: DB 테이블명 지정
 * @Id: Primary Key (테이블의 고유 식별자)
 * @ManyToOne: 다대일 관계 (여러 식단은 한 사용자에 속함)
 * @Column: 컬럼의 속성 지정 (NOT NULL, 길이 등)
 */
@Entity
@Table(name = "meal_plans")  // DB 테이블명: meal_plans
@Getter  // Lombok: 모든 필드의 getter 자동 생성
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // Lombok: 파라미터 없는 생성자 생성 (JPA가 필요로 함)
public class MealPlan {

    /**
     * 식단 ID (Primary Key, 자동 증가)
     *
     * @GeneratedValue: 자동 증가 설정 (1, 2, 3, ...)
     * @Id: 이 필드가 PK임을 표시
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long mealPlanId;

    /**
     * 이 식단이 속한 사용자
     *
     * @ManyToOne: User 한 명이 여러 MealPlan을 가질 수 있음
     * @JoinColumn: FK(Foreign Key) 컬럼명 지정
     * fetch = FetchType.LAZY: 필요할 때만 로드 (성능 최적화)
     * nullable = false: 반드시 사용자가 있어야 함
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 사용자의 목표
     * 예: "WEIGHT_LOSS", "MUSCLE_GAIN", "DIET_BALANCE"
     */
    @Column(nullable = false, length = 20)
    private String goal;

    /**
     * 식단 계획 기간 (일)
     * 예: 7일, 30일, 365일 등
     */
    @Column(nullable = false)
    private Integer durationDays;


    /**
     * 사용자의 키 (센티미터)
     */
    @Column(nullable = false)
    private Integer heightCm;

    /**
     * 사용자의 체중 (킬로그램)
     */
    @Column(nullable = false)
    private Integer weightKg;

    /**
     * 사용자의 나이 (세)
     */
    @Column(nullable = false)
    private Integer age;

    /**
     * 사용자의 성별
     * 예: "MALE", "FEMALE"
     */
    @Column(nullable = false, length = 10)
    private String gender;

    /**
     * 사용자의 bmi
     * 예: 22.86
     */
    @Column(nullable = false)
    private BigDecimal bmi;

    /**
     * 계산된 목표 칼로리 (kcal)
     * 사용자의 목표에 따라 조정된 칼로리
     * 예: 2000, 2300 등
     */
    @Column(nullable = false)
    private Integer targetCalories;

    /**
     * 기초대사량 (BMR, Basal Metabolic Rate)
     * 아무 활동을 하지 않을 때 몸이 소비하는 칼로리
     * 예: 1649 kcal
     */
    @Column(nullable = false)
    private Integer bmr;

    /**
     * 일일 총 소비 칼로리 (TDEE, Total Daily Energy Expenditure)
     * 일상 활동을 고려한 하루 소비 칼로리
     * 예: 2226 kcal
     */
    @Column(nullable = false)
    private Integer tdee;

    /**
     * 하루 단백질 섭취량 (그램)
     * 예: 112g
     */
    @Column(nullable = false)
    private Integer proteinGram;

    /**
     * 하루 탄수화물 섭취량 (그램)
     * 예: 262g
     */
    @Column(nullable = false)
    private Integer carbsGram;

    /**
     * 하루 지방 섭취량 (그램)
     * 예: 56g
     */
    @Column(nullable = false)
    private Integer fatGram;

    /**
     * 체지방률
     * 예: 26.2%
     */
    @Column
    private BigDecimal bodyFatRate;

    /**
     * AI가 생성한 식단 데이터 (JSON 문자열)
     *
     * @Lob: Large Object로 취급 (큰 데이터 저장)
     * columnDefinition = "JSON": MySQL JSON 타입으로 저장
     *
     * 예시:
     * {
     *   "days": [
     *     {
     *       "dayNumber": 1,
     *       "breakfast": {"name": "계란말이...", "calories": 400, ...},
     *       "lunch": {...},
     *       "dinner": {...}
     *     },
     *     ...
     *   ]
     * }
     */
    @Lob
    @Column(nullable = false, columnDefinition = "JSON")
    private String aiResponseJson;

    /**
     * 식단 시작 날짜 및 시간
     * 예: 2024-05-20 10:30:00
     */
    @Column(nullable = false)
    private LocalDateTime startedAt;

    /**
     * 식단 종료 날짜 및 시간
     * 예: 2024-05-27 10:30:00
     */
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    /**
     * 생성 시간 (자동으로 현재 시간 설정)
     *
     * @CreationTimestamp: 엔티티가 생성될 때 자동으로 현재 시간 저장
     * 이후 값 변경 안 됨
     * 예: 2024-05-20 10:30:15
     */
    @CreationTimestamp
    private LocalDateTime createdAt;

    /**
     * 수정 시간 (엔티티가 수정될 때마다 자동으로 업데이트)
     *
     * @UpdateTimestamp: 엔티티가 수정될 때마다 자동으로 현재 시간으로 업데이트
     * 예: 2024-05-20 10:30:15 (처음 생성시) → 2024-05-21 15:45:20 (수정시)
     */
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /**
     * MealPlan 생성자
     *
     * Service에서 새로운 식단 정보를 저장할 때 호출됩니다
     * 모든 필드를 파라미터로 받아 초기화합니다
     *
     * @param user 사용자
     * @param goal 목표
     * @param durationDays 기간
     * @param heightCm 키
     * @param weightKg 체중
     * @param age 나이
     * @param gender 성별
     * @param targetCalories 목표 칼로리
     * @param bmr 기초대사량
     * @param tdee 일일 총 소비 칼로리
     * @param proteinGram 단백질
     * @param carbsGram 탄수화물
     * @param fatGram 지방
     * @param bodyFatRate 체지방률
     * @param aiResponseJson AI 응답 (JSON)
     * @param startedAt 시작 시간
     * @param expiresAt 종료 시간
     */
    @Builder
    public MealPlan(
            User user,
            String goal,
            Integer durationDays,
            Integer heightCm,
            Integer weightKg,
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
            LocalDateTime startedAt,
            LocalDateTime expiresAt
    ) {
        this.user = user;
        this.goal = goal;
        this.durationDays = durationDays;
        this.heightCm = heightCm;
        this.weightKg = weightKg;
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
        this.startedAt = startedAt;
        this.expiresAt = expiresAt;
    }
}