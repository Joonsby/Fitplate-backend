// 식단에 속한 끼니와 음식의 표시 순서를 관리합니다.
package com.fitplate.fitplateapi.mealplan.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "meal_plan_meals", uniqueConstraints = {
        @UniqueConstraint(name = "uk_meal_plan_meals_plan_type", columnNames = {"meal_plan_id", "meal_type"}),
        @UniqueConstraint(name = "uk_meal_plan_meals_plan_order", columnNames = {"meal_plan_id", "sort_order"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MealPlanMeal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "meal_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meal_plan_id", nullable = false)
    private MealPlan mealPlan;

    @Column(name = "meal_type", nullable = false, length = 20)
    private String mealType;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @OneToMany(mappedBy = "meal", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @BatchSize(size = 50)
    private List<MealPlanFood> foods = new ArrayList<>();

    @Builder
    public MealPlanMeal(String mealType, String title, Integer sortOrder) {
        this.mealType = mealType;
        this.title = title;
        this.sortOrder = sortOrder;
    }

    void assignTo(MealPlan mealPlan) {
        this.mealPlan = mealPlan;
    }

    public void addFood(MealPlanFood food) {
        foods.add(food);
        food.assignTo(this);
    }
}
