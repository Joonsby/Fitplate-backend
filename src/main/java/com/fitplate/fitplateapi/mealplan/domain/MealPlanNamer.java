// 식단 음식으로 자동 이름을 만듭니다. 점심·저녁에서 단백질이 가장 많은 음식을 대표로 씁니다. 예) 닭가슴살 구이·연어 스테이크 감량 식단
package com.fitplate.fitplateapi.mealplan.domain;

import com.fitplate.fitplateapi.mealplan.dto.FoodItem;
import com.fitplate.fitplateapi.mealplan.dto.MealPlanResponse;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class MealPlanNamer {

    public static final int MAX_LENGTH = 30;
    private static final int MAX_DISH_LENGTH = 10;
    private static final List<String> MAIN_MEALS = List.of("lunch", "dinner", "breakfast");

    private MealPlanNamer() {
    }

    public static String defaultName(MealPlan plan) {
        return name(plan.getGoal(), mealType -> plan.getMeals().stream()
                .filter(meal -> mealType.equals(meal.getMealType()))
                .flatMap(meal -> meal.getFoods().stream())
                .max(Comparator.comparing(food -> food.getProtein() == null ? BigDecimal.ZERO : food.getProtein()))
                .map(MealPlanFood::getName));
    }

    /** 아직 저장하지 않은 AI 응답으로 같은 규칙의 이름을 만든다. */
    public static String defaultName(String goal, MealPlanResponse response) {
        return name(goal, mealType -> response.getMeals() == null ? Optional.empty() : response.getMeals().stream()
                .filter(meal -> mealType.equals(meal.getMealType()) && meal.getFoods() != null)
                .flatMap(meal -> meal.getFoods().stream())
                .max(Comparator.comparingDouble(food -> food.getProtein() == null ? 0 : food.getProtein()))
                .map(FoodItem::getName));
    }

    private static String name(String goal, Function<String, Optional<String>> mainDishOf) {
        String suffix = goalLabel(goal) + " 식단";
        List<String> dishes = new ArrayList<>();
        for (String mealType : MAIN_MEALS) {
            if (dishes.size() == 2) {
                break;
            }
            mainDishOf.apply(mealType)
                    .map(String::trim)
                    .filter(dish -> !dish.isEmpty())
                    .map(MealPlanNamer::shorten)
                    .filter(dish -> dishes.stream().noneMatch(dish::equals))
                    .ifPresent(dishes::add);
        }
        if (dishes.isEmpty()) {
            return suffix;
        }
        String both = String.join("·", dishes) + " " + suffix;
        return both.length() <= MAX_LENGTH ? both : dishes.get(0) + " " + suffix;
    }

    private static String shorten(String name) {
        return name.length() <= MAX_DISH_LENGTH ? name : name.substring(0, MAX_DISH_LENGTH);
    }

    private static String goalLabel(String goal) {
        if ("WEIGHT_LOSS".equals(goal)) {
            return "감량";
        }
        if ("WEIGHT_GAIN".equals(goal) || "MUSCLE_GAIN".equals(goal)) {
            return "증량";
        }
        return "유지";
    }
}
