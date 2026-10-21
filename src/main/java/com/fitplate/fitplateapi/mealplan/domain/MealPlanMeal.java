// 식단에 속한 끼니와 음식의 표시 순서를 관리합니다.
package com.fitplate.fitplateapi.mealplan.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
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

    /** 먹었다고 체크한 시각. null 이면 미섭취. */
    @Column(name = "eaten_at")
    private LocalDateTime eatenAt;

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

    /** 끼니 재생성 시 기존 음식을 모두 지운다. 새 음식을 넣기 전에 flush 해야 순서 유니크 키가 겹치지 않는다. */
    public void clearFoods() {
        foods.clear();
    }

    public boolean isEaten() {
        return eatenAt != null;
    }

    /** 이미 먹은 끼니면 처음 체크한 시각을 유지한다. */
    public void markEaten(LocalDateTime now) {
        if (eatenAt == null) {
            eatenAt = now;
        }
    }

    public void clearEaten() {
        eatenAt = null;
    }

    public void rename(String title) {
        this.title = title;
    }

    void assignTo(MealPlan mealPlan) {
        this.mealPlan = mealPlan;
    }

    public void addFood(MealPlanFood food) {
        foods.add(food);
        food.assignTo(this);
    }
}
