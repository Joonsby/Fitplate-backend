// AI 식단이 형식·영양 목표·제외·알레르기 음식 조건을 지켰는지 검사합니다.
package com.fitplate.fitplateapi.mealplan.service;

import com.fitplate.fitplateapi.ai.MealRegenerationContext.MealTarget;
import com.fitplate.fitplateapi.mealplan.dto.FoodItem;
import com.fitplate.fitplateapi.mealplan.dto.MealPlanResponse;
import com.fitplate.fitplateapi.mealplan.dto.MealResponse;
import com.fitplate.fitplateapi.nutrition.dto.NutritionResult;
import com.fitplate.fitplateapi.user.domain.FoodCategory;
import com.fitplate.fitplateapi.user.domain.FoodPreferences;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class MealPlanValidator {

    static final double CALORIE_TOLERANCE = 0.10;
    static final double MACRO_TOLERANCE = 0.20;
    private static final Set<String> MEAL_TYPES = Set.of("breakfast", "lunch", "dinner");

    public Result validate(MealPlanResponse plan, NutritionResult target, FoodPreferences preferences) {
        if (!isWellFormed(plan)) {
            return Result.malformed();
        }

        List<String> problems = new ArrayList<>();
        double score = 0;
        List<FoodItem> foods = plan.getMeals().stream().flatMap(meal -> meal.getFoods().stream()).toList();

        int calories = foods.stream().mapToInt(FoodItem::getCalories).sum();
        double protein = foods.stream().mapToDouble(FoodItem::getProtein).sum();
        double carbs = foods.stream().mapToDouble(FoodItem::getCarbohydrate).sum();
        double fat = foods.stream().mapToDouble(FoodItem::getFat).sum();

        score += checkTotal("하루 총 칼로리", calories, target.getTargetCalories(), "kcal", CALORIE_TOLERANCE, problems);
        score += checkTotal("하루 총 단백질", protein, target.getProteinGram(), "g", MACRO_TOLERANCE, problems);
        score += checkTotal("하루 총 탄수화물", carbs, target.getCarbsGram(), "g", MACRO_TOLERANCE, problems);
        score += checkTotal("하루 총 지방", fat, target.getFatGram(), "g", MACRO_TOLERANCE, problems);

        score += checkExcluded(foods, preferences, problems);
        return new Result(true, List.copyOf(problems), score, hasAllergy(foods, preferences));
    }

    /**
     * 한 끼 재생성 결과를 검사한다. 끼니 단위는 숫자가 작아 칼로리 30kcal·영양소 5g 까지의 차이는 봐준다.
     */
    public Result validateMeal(MealResponse meal, String expectedMealType,
                               MealTarget target, FoodPreferences preferences) {
        if (meal == null || !expectedMealType.equals(meal.getMealType()) || meal.getTitle() == null
                || meal.getFoods() == null || meal.getFoods().isEmpty()
                || !meal.getFoods().stream().allMatch(MealPlanValidator::isWellFormed)) {
            return new Result(false, List.of(
                    "mealType은 " + expectedMealType + " 이어야 하고, 음식이 1개 이상 있어야 하며 모든 음식 필드를 0 이상의 값으로 채워야 한다"
            ), Double.MAX_VALUE);
        }

        List<String> problems = new ArrayList<>();
        double score = 0;
        List<FoodItem> foods = meal.getFoods();
        score += checkTotal("끼니 총 칼로리", foods.stream().mapToInt(FoodItem::getCalories).sum(),
                target.calories(), "kcal", CALORIE_TOLERANCE, 30, problems);
        score += checkTotal("끼니 총 단백질", foods.stream().mapToDouble(FoodItem::getProtein).sum(),
                target.protein(), "g", MACRO_TOLERANCE, 5, problems);
        score += checkTotal("끼니 총 탄수화물", foods.stream().mapToDouble(FoodItem::getCarbohydrate).sum(),
                target.carbohydrate(), "g", MACRO_TOLERANCE, 5, problems);
        score += checkTotal("끼니 총 지방", foods.stream().mapToDouble(FoodItem::getFat).sum(),
                target.fat(), "g", MACRO_TOLERANCE, 5, problems);
        score += checkExcluded(foods, preferences, problems);
        return new Result(true, List.copyOf(problems), score, hasAllergy(foods, preferences));
    }

    private double checkExcluded(List<FoodItem> foods, FoodPreferences preferences, List<String> problems) {
        double score = 0;
        for (FoodItem food : foods) {
            FoodCategory allergy = preferences.findAllergyMatch(food.getName(), food.getShoppingKeyword());
            if (allergy != null) {
                problems.add("'%s'은(는) 사용자의 알레르기 분류(%s)에 해당한다. 반드시 빼고 다른 음식으로 바꿔라".formatted(food.getName(), allergy.getLabel()));
                score += 1;
                continue;
            }
            FoodCategory hit = preferences.findExcludedMatch(food.getName(), food.getShoppingKeyword());
            if (hit != null) {
                problems.add("'%s'은(는) 제외한 분류(%s)에 해당한다. 빼고 다른 음식으로 바꿔라".formatted(food.getName(), hit.getLabel()));
                score += 1;
            }
        }
        return score;
    }

    private static boolean hasAllergy(List<FoodItem> foods, FoodPreferences preferences) {
        return foods.stream().anyMatch(food -> preferences.findAllergyMatch(food.getName(), food.getShoppingKeyword()) != null);
    }

    private double checkTotal(String label, double actual, double target, String unit, double tolerance,
                              double minAllowed, List<String> problems) {
        if (target <= 0) {
            return 0;
        }
        double allowed = Math.max(target * tolerance, minAllowed);
        if (Math.abs(actual - target) > allowed) {
            problems.add("%s이(가) %d%s로 목표 %d%s와 %d%s 차이난다".formatted(
                    label, Math.round(actual), unit, Math.round(target), unit, Math.round(Math.abs(actual - target)), unit));
            return Math.abs(actual - target) / target;
        }
        return 0;
    }

    private double checkTotal(String label, double actual, int target, String unit, double tolerance, List<String> problems) {
        if (target <= 0) {
            return 0;
        }
        double deviation = Math.abs(actual - target) / target;
        if (deviation > tolerance) {
            problems.add("%s이(가) %d%s로 목표 %d%s에서 %d퍼센트 벗어났다".formatted(
                    label, Math.round(actual), unit, target, unit, Math.round(deviation * 100)));
        }
        return deviation > tolerance ? deviation : 0;
    }

    private boolean isWellFormed(MealPlanResponse plan) {
        if (plan == null || plan.getMeals() == null || plan.getMeals().size() != MEAL_TYPES.size()) {
            return false;
        }
        Set<String> types = plan.getMeals().stream()
                .map(meal -> meal == null ? null : meal.getMealType())
                .collect(Collectors.toSet());
        if (!types.equals(MEAL_TYPES)) {
            return false;
        }
        for (MealResponse meal : plan.getMeals()) {
            if (meal.getTitle() == null || meal.getFoods() == null || meal.getFoods().isEmpty()) {
                return false;
            }
            for (FoodItem food : meal.getFoods()) {
                if (!isWellFormed(food)) {
                    return false;
                }
            }
        }
        return true;
    }

    static boolean isWellFormed(FoodItem food) {
        return food != null
                && food.getName() != null && !food.getName().isBlank()
                && food.getAmount() != null && !food.getAmount().isBlank()
                && food.getShoppingKeyword() != null
                && food.getCalories() != null && food.getCalories() >= 0
                && food.getProtein() != null && food.getProtein() >= 0
                && food.getCarbohydrate() != null && food.getCarbohydrate() >= 0
                && food.getFat() != null && food.getFat() >= 0;
    }

    /**
     * wellFormed 가 false 거나 allergyViolation 이 true 면 쓸 수 없는 결과다.
     * score 는 목표에서 벗어난 정도로, 낮을수록 좋다.
     */
    public record Result(boolean wellFormed, List<String> problems, double score, boolean allergyViolation) {

        public Result(boolean wellFormed, List<String> problems, double score) {
            this(wellFormed, problems, score, false);
        }

        static Result malformed() {
            return new Result(false, List.of(
                    "meals 배열에 breakfast, lunch, dinner 3끼가 하나씩 있어야 하고, 각 끼니에 음식이 1개 이상 있어야 한다",
                    "모든 음식은 name, amount, calories, protein, carbohydrate, fat, shoppingKeyword를 0 이상의 값으로 채워야 한다"
            ), Double.MAX_VALUE);
        }

        public boolean isAcceptable() {
            return wellFormed && problems.isEmpty();
        }

        /** 목표에서 조금 벗어나도 저장은 할 수 있는 결과인지. 알레르기 위반은 저장하지 않는다. */
        public boolean isUsable() {
            return wellFormed && !allergyViolation;
        }

        public String feedback() {
            return problems.stream().map(problem -> "- " + problem).collect(Collectors.joining("\n"));
        }
    }
}
