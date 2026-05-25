package com.fitplate.fitplateapi.user.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_profiles",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_profiles_toss_user_key",
                        columnNames = "toss_user_key"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_profile_id")
    private Long userProfileId;

    @Column(name = "toss_user_key", nullable = false, length = 100)
    private String tossUserKey;

    @Column(name = "height_cm", nullable = false)
    private Integer heightCm;

    @Column(name = "weight_kg", nullable = false)
    private Integer weightKg;

    @Column(name = "age", nullable = false)
    private Integer age;

    @Column(name = "gender", nullable = false, length = 10)
    private String gender;

    @Column(name = "bmi", precision = 5, scale = 2)
    private BigDecimal bmi;

    @Column(name = "body_fat_rate", precision = 5, scale = 2)
    private BigDecimal bodyFatRate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public UserProfile(
            String tossUserKey,
            Integer heightCm,
            Integer weightKg,
            Integer age,
            String gender,
            BigDecimal bmi,
            BigDecimal bodyFatRate
    ) {
        this.tossUserKey = tossUserKey;
        this.heightCm = heightCm;
        this.weightKg = weightKg;
        this.age = age;
        this.gender = gender;
        this.bmi = bmi;
        this.bodyFatRate = bodyFatRate;
    }

    public void update(
            Integer heightCm,
            Integer weightKg,
            Integer age,
            String gender,
            BigDecimal bmi,
            BigDecimal bodyFatRate
    ) {
        this.heightCm = heightCm;
        this.weightKg = weightKg;
        this.age = age;
        this.gender = gender;
        this.bmi = bmi;
        this.bodyFatRate = bodyFatRate;
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