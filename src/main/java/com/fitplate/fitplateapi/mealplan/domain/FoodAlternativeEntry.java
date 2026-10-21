// AI 가 만든 음식 대안 하나입니다. 사용자와 묶지 않아 같은 음식을 바꾸는 모든 사용자가 다시 씁니다.
package com.fitplate.fitplateapi.mealplan.domain;

import com.fitplate.fitplateapi.mealplan.dto.FoodItem;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "food_alternative_pool", uniqueConstraints = {
        @UniqueConstraint(name = "uk_food_alternative_pool_source_candidate", columnNames = {"source_key", "candidate_key"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FoodAlternativeEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "entry_id")
    private Long id;

    @Column(name = "source_key", nullable = false)
    private String sourceKey;

    @Column(name = "candidate_key", nullable = false)
    private String candidateKey;

    @Column(nullable = false)
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

    @Column(name = "shopping_keyword", nullable = false)
    private String shoppingKeyword;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public FoodAlternativeEntry(String sourceKey, String candidateKey, FoodItem food, LocalDateTime createdAt) {
        this.sourceKey = sourceKey;
        this.candidateKey = candidateKey;
        this.name = food.getName();
        this.amount = food.getAmount();
        this.calories = food.getCalories();
        this.protein = decimal(food.getProtein());
        this.carbohydrate = decimal(food.getCarbohydrate());
        this.fat = decimal(food.getFat());
        this.shoppingKeyword = food.getShoppingKeyword();
        this.createdAt = createdAt;
    }

    public FoodItem toFoodItem() {
        return FoodItem.builder()
                .name(name)
                .amount(amount)
                .calories(calories)
                .protein(protein.doubleValue())
                .carbohydrate(carbohydrate.doubleValue())
                .fat(fat.doubleValue())
                .shoppingKeyword(shoppingKeyword)
                .build();
    }

    private static BigDecimal decimal(Double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}
