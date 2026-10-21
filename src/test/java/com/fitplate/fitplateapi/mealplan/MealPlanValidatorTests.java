// AI 식단 검증기의 형식·영양 목표·제외 음식 판정을 검증합니다.
package com.fitplate.fitplateapi.mealplan;

import com.fitplate.fitplateapi.mealplan.dto.FoodItem;
import com.fitplate.fitplateapi.mealplan.dto.MealPlanResponse;
import com.fitplate.fitplateapi.mealplan.dto.MealResponse;
import com.fitplate.fitplateapi.mealplan.service.MealPlanValidator;
import com.fitplate.fitplateapi.nutrition.dto.NutritionResult;
import com.fitplate.fitplateapi.user.domain.FoodCategory;
import com.fitplate.fitplateapi.user.domain.FoodPreferences;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class MealPlanValidatorTests {

    private final MealPlanValidator validator = new MealPlanValidator();

    /** 끼니마다 700kcal · 단백질 40g · 탄수화물 90g · 지방 20g → 하루 2100 / 120 / 270 / 60 */
    public static MealPlanResponse balancedPlan(String lunchMain) {
        return MealPlanResponse.builder().meals(List.of(
                meal("breakfast", "현미밥", "닭가슴살"),
                meal("lunch", "현미밥", lunchMain),
                meal("dinner", "고구마", "두부")
        )).build();
    }

    public static MealResponse meal(String type, String carb, String main) {
        return MealResponse.builder().mealType(type).title(type).foods(List.of(
                food(carb, 400, 10, 80, 5),
                food(main, 300, 30, 10, 15)
        )).build();
    }

    public static FoodItem food(String name, int calories, double protein, double carbs, double fat) {
        return FoodItem.builder().name(name).amount("1인분").calories(calories)
                .protein(protein).carbohydrate(carbs).fat(fat).shoppingKeyword(name).build();
    }

    public static NutritionResult target() {
        return NutritionResult.builder().bmr(1600).tdee(2100).targetCalories(2100)
                .proteinGram(120).carbsGram(270).fatGram(60).build();
    }

    @Test
    void balancedPlanIsAcceptable() {
        MealPlanValidator.Result result = validator.validate(balancedPlan("연어구이"), target(), FoodPreferences.none());

        assertThat(result.isAcceptable()).isTrue();
        assertThat(result.score()).isZero();
    }

    @Test
    void calorieDeviationOverTenPercentIsReported() {
        NutritionResult higherTarget = NutritionResult.builder().bmr(1600).tdee(2600).targetCalories(2600)
                .proteinGram(120).carbsGram(270).fatGram(60).build();

        MealPlanValidator.Result result = validator.validate(balancedPlan("연어구이"), higherTarget, FoodPreferences.none());

        assertThat(result.wellFormed()).isTrue();
        assertThat(result.isAcceptable()).isFalse();
        assertThat(result.feedback()).contains("하루 총 칼로리").contains("2100kcal").contains("2600kcal");
    }

    @Test
    void excludedCategoryFoodIsReported() {
        FoodPreferences noPork = FoodPreferences.of(List.of(), List.of(FoodCategory.PORK));

        MealPlanValidator.Result result = validator.validate(balancedPlan("제육볶음"), target(), noPork);

        assertThat(result.isAcceptable()).isFalse();
        assertThat(result.feedback()).contains("제육볶음").contains("돼지고기");
    }

    @Test
    void missingMealOrFieldIsMalformed() {
        MealPlanResponse twoMeals = MealPlanResponse.builder().meals(List.of(
                meal("breakfast", "현미밥", "닭가슴살"), meal("lunch", "현미밥", "연어구이"))).build();
        MealPlanResponse nullCalories = MealPlanResponse.builder().meals(List.of(
                meal("breakfast", "현미밥", "닭가슴살"),
                meal("lunch", "현미밥", "연어구이"),
                MealResponse.builder().mealType("dinner").title("dinner").foods(List.of(
                        FoodItem.builder().name("고구마").amount("1개").protein(1.0).carbohydrate(1.0).fat(1.0)
                                .shoppingKeyword("고구마").build())).build()
        )).build();

        assertThat(validator.validate(twoMeals, target(), FoodPreferences.none()).wellFormed()).isFalse();
        assertThat(validator.validate(nullCalories, target(), FoodPreferences.none()).wellFormed()).isFalse();
        assertThat(validator.validate(null, target(), FoodPreferences.none()).wellFormed()).isFalse();
    }
}
