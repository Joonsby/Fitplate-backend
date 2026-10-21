package com.fitplate.fitplateapi.mealplan.domain;

import com.fitplate.fitplateapi.user.domain.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 식단 계획 Entity (meal_plans 테이블 매핑).
 */
@Entity
@Table(name = "meal_plans", uniqueConstraints = {
        @UniqueConstraint(name = "uk_meal_plans_user_plan_date", columnNames = {"user_id", "plan_date"})
})
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

    /** 사용자의 목표 (MAINTAIN, WEIGHT_LOSS, WEIGHT_GAIN). */
    @Column(nullable = false, length = 20)
    private String goal;

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

    @OneToMany(mappedBy = "mealPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<MealPlanMeal> meals = new ArrayList<>();

    public void addMeal(MealPlanMeal meal) {
        meals.add(meal);
        meal.assignTo(this);
    }

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

    /** 이 식단을 따르는 날짜. 사용자당 하루 하나이고, null 이면 날짜 미지정. */
    @Column(name = "plan_date")
    private LocalDate planDate;

    /** 사용자가 지정한 이름. null 이면 displayTitle() 이 자동 이름을 만든다. */
    @Column(length = 30)
    private String title;

    public void schedule(LocalDate planDate) {
        this.planDate = planDate;
    }

    /** 같은 음식으로 새 식단을 만든다. 날짜와 섭취 기록은 옮기지 않고, 직접 정한 이름은 그대로 쓴다. */
    public MealPlan copy() {
        MealPlan copy = MealPlan.builder()
                .user(user).goal(goal).height(height).weight(weight).age(age).gender(gender)
                .bmi(bmi).bodyFatRate(bodyFatRate)
                .targetCalories(targetCalories).bmr(bmr).tdee(tdee)
                .proteinGram(proteinGram).carbsGram(carbsGram).fatGram(fatGram)
                .aiResponseJson(aiResponseJson)
                .build();
        copy.title = title;
        for (MealPlanMeal meal : meals) {
            MealPlanMeal mealCopy = MealPlanMeal.builder()
                    .mealType(meal.getMealType()).title(meal.getTitle()).sortOrder(meal.getSortOrder()).build();
            copy.addMeal(mealCopy);
            for (MealPlanFood food : meal.getFoods()) {
                mealCopy.addFood(MealPlanFood.builder()
                        .name(food.getName()).amount(food.getAmount()).calories(food.getCalories())
                        .protein(food.getProtein()).carbohydrate(food.getCarbohydrate()).fat(food.getFat())
                        .shoppingKeyword(food.getShoppingKeyword()).sortOrder(food.getSortOrder())
                        .build());
            }
        }
        return copy;
    }

    /** 비어 있으면 자동 이름으로 되돌린다. */
    public void rename(String title) {
        this.title = title == null || title.isBlank() ? null : title.trim();
    }

    public String displayTitle() {
        return title != null ? title : MealPlanNamer.defaultName(this);
    }

    public boolean hasEatenMeal() {
        return meals.stream().anyMatch(MealPlanMeal::isEaten);
    }


    /** Service에서 식단 저장 시 모든 필드를 받아 초기화. */
    @Builder
    public MealPlan(
            User user,
            String goal,
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
            String mealPlanHash
    ) {
        this.user = user;
        this.goal = goal;
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
    }
}
