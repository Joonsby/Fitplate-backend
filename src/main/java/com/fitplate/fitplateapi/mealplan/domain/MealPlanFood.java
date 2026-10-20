// 끼니별 음식의 분량과 생성 당시 영양 정보를 보관합니다.
package com.fitplate.fitplateapi.mealplan.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "meal_plan_foods", uniqueConstraints = {
        @UniqueConstraint(name = "uk_meal_plan_foods_meal_order", columnNames = {"meal_id", "sort_order"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MealPlanFood {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "food_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meal_id", nullable = false)
    private MealPlanMeal meal;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 100)
    private String amount;

    @Column(nullable = false)
    private Integer calories;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal protein;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal carbohydrate;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal fat;

    @Column(name = "shopping_keyword", nullable = false, length = 255)
    private String shoppingKeyword;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Builder
    public MealPlanFood(String name, String amount, Integer calories, BigDecimal protein,
                        BigDecimal carbohydrate, BigDecimal fat, String shoppingKeyword, Integer sortOrder) {
        this.name = name;
        this.amount = amount;
        this.calories = calories;
        this.protein = protein;
        this.carbohydrate = carbohydrate;
        this.fat = fat;
        this.shoppingKeyword = shoppingKeyword;
        this.sortOrder = sortOrder;
    }

    void assignTo(MealPlanMeal meal) {
        this.meal = meal;
    }
}
