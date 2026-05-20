package com.fitplate.fitplateapi.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "meal_plans")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MealPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long mealPlanId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 20)
    private String goal;

    @Column(nullable = false)
    private Integer durationDays;

    @Column(nullable = false)
    private Integer heightCm;

    @Column(nullable = false)
    private Integer weightKg;

    @Column(nullable = false)
    private Integer age;

    @Column(nullable = false, length = 10)
    private String gender;

    @Column(nullable = false)
    private Integer targetCalories;

    @Column(nullable = false)
    private Integer bmr;

    @Column(nullable = false)
    private Integer tdee;

    @Column(nullable = false)
    private Integer proteinGram;

    @Column(nullable = false)
    private Integer carbsGram;

    @Column(nullable = false)
    private Integer fatGram;

    @Lob
    @Column(nullable = false, columnDefinition = "JSON")
    private String aiResponseJson;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public MealPlan(
            User user,
            String goal,
            Integer durationDays,
            Integer heightCm,
            Integer weightKg,
            Integer age,
            String gender,
            Integer targetCalories,
            Integer bmr,
            Integer tdee,
            Integer proteinGram,
            Integer carbsGram,
            Integer fatGram,
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
        this.targetCalories = targetCalories;
        this.bmr = bmr;
        this.tdee = tdee;
        this.proteinGram = proteinGram;
        this.carbsGram = carbsGram;
        this.fatGram = fatGram;
        this.aiResponseJson = aiResponseJson;
        this.startedAt = startedAt;
        this.expiresAt = expiresAt;
    }
}