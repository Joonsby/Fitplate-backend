package com.fitplate.fitplateapi.user.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

@Entity
@Table(name = "user_profiles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserProfile {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "height", nullable = false)
    private Integer height;

    @Column(name = "weight", nullable = false)
    private Integer weight;

    @Column(name = "age", nullable = false)
    private Integer age;

    @Column(name = "gender", nullable = false, length = 10)
    private String gender;

    @Column(name = "bmi", precision = 5, scale = 2)
    private BigDecimal bmi;

    @Column(name = "body_fat_rate", precision = 5, scale = 2)
    private BigDecimal bodyFatRate;

    @Convert(converter = FoodCategorySetConverter.class)
    @Column(name = "preferred_food_categories", length = 255)
    private Set<FoodCategory> preferredFoodCategories = EnumSet.noneOf(FoodCategory.class);

    @Convert(converter = FoodCategorySetConverter.class)
    @Column(name = "excluded_food_categories", length = 255)
    private Set<FoodCategory> excludedFoodCategories = EnumSet.noneOf(FoodCategory.class);

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public UserProfile(
            User user,
            Integer height,
            Integer weight,
            Integer age,
            String gender,
            BigDecimal bmi,
            BigDecimal bodyFatRate
    ) {
        this.user = user;
        this.height = height;
        this.weight = weight;
        this.age = age;
        this.gender = gender;
        this.bmi = bmi;
        this.bodyFatRate = bodyFatRate;
    }

    public void update(
            Integer height,
            Integer weight,
            Integer age,
            String gender,
            BigDecimal bmi,
            BigDecimal bodyFatRate
    ) {
        this.height = height;
        this.weight = weight;
        this.age = age;
        this.gender = gender;
        this.bmi = bmi;
        this.bodyFatRate = bodyFatRate;
    }

    /** 목표 체중(kg). null 이면 설정 안 함. */
    @Column(name = "target_weight", precision = 5, scale = 2)
    private BigDecimal targetWeight;

    public void updateTargetWeight(BigDecimal targetWeight) {
        this.targetWeight = targetWeight;
    }

    @Convert(converter = FoodCategorySetConverter.class)
    @Column(name = "allergy_food_categories", length = 255)
    private Set<FoodCategory> allergyFoodCategories = EnumSet.noneOf(FoodCategory.class);

    public FoodPreferences getFoodPreferences() {
        return FoodPreferences.of(preferredFoodCategories, excludedFoodCategories, allergyFoodCategories);
    }

    public void updateFoodPreferences(Set<FoodCategory> preferred, Set<FoodCategory> excluded) {
        updateFoodPreferences(preferred, excluded, null);
    }

    /** null 인 쪽은 기존 값을 유지한다. 겹치면 알레르기 > 제외 > 선호 순으로 남는다. */
    public void updateFoodPreferences(Set<FoodCategory> preferred, Set<FoodCategory> excluded,
                                      Set<FoodCategory> allergies) {
        FoodPreferences next = FoodPreferences.of(
                preferred != null ? preferred : preferredFoodCategories,
                excluded != null ? excluded : excludedFoodCategories,
                allergies != null ? allergies : allergyFoodCategories
        );
        this.preferredFoodCategories = EnumSet.noneOf(FoodCategory.class);
        this.preferredFoodCategories.addAll(next.preferred());
        this.excludedFoodCategories = EnumSet.noneOf(FoodCategory.class);
        this.excludedFoodCategories.addAll(next.excluded());
        this.allergyFoodCategories = EnumSet.noneOf(FoodCategory.class);
        this.allergyFoodCategories.addAll(next.allergies());
    }

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}