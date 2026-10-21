// OpenAI·Gemini 가 같이 쓰는 식단 생성·음식 대안 프롬프트를 만듭니다. 알레르기는 제외보다 강하게 지시합니다.
package com.fitplate.fitplateapi.ai;

import com.fitplate.fitplateapi.mealplan.dto.FoodItem;
import com.fitplate.fitplateapi.mealplan.dto.MealPlanRequest;
import com.fitplate.fitplateapi.nutrition.dto.NutritionResult;
import com.fitplate.fitplateapi.user.domain.FoodCategory;
import com.fitplate.fitplateapi.user.domain.FoodPreferences;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

final class MealPlanPrompts {

    private MealPlanPrompts() {
    }

    static String generation(MealPlanRequest request, NutritionResult nutritionResult,
                             FoodPreferences preferences, PersonalizationHints hints, String feedback) {
        return """
            너는 전문 식단 추천 API다.

            사용자의 신체 정보와 목표를 바탕으로 한국식 식단을 생성해라.

            반드시 아래 JSON 구조만 반환해라.
            설명 문장, 마크다운, 코드블록은 절대 반환하지 마라.

        조건:
        - 하루 식단 1개만 생성해라.
        - meals 배열에는 breakfast, lunch, dinner 3끼를 모두 포함해라.
        - mealType은 반드시 breakfast, lunch, dinner 중 하나만 사용해라.
        - foods 배열의 각 원소는 반드시 하나의 음식만 표현해야 한다.
        - 음식명은 "현미밥과 닭가슴살 미역국"처럼 합치지 말고 반드시 개별 음식으로 분리해라.
        - 각 음식은 name, amount, calories, protein, carbohydrate, fat, shoppingKeyword를 포함해라.
        - shoppingKeyword는 쿠팡 같은 쇼핑몰에서 검색하기 좋은 짧은 키워드로 작성해라.
        - 하루 총 칼로리는 targetCalories의 ±10퍼센트 이내로 맞춰라.
        - 하루 총 단백질은 proteinGram의 ±10퍼센트 이내로 맞춰라.
        - 하루 총 탄수화물은 carbsGram의 ±10퍼센트 이내로 맞춰라.
        - 하루 총 지방은 fatGram의 ±10퍼센트 이내로 맞춰라.
        - goal이 WEIGHT_LOSS면 감량식으로 구성해라.
        - 한국에서 일반적으로 구하기 쉬운 식재료 위주로 구성해라.
        %s%s
        사용자 정보:
        height=%s
        weight=%s
        gender=%s
        age=%s
        bodyFatRate=%s
        goal=%s
        targetCalories=%s
        proteinGram=%s
        carbsGram=%s
        fatGram=%s
        %s""".formatted(
                preferenceRules(preferences),
                hintSection(hints),
                request.getHeight(),
                request.getWeight(),
                request.getGender(),
                request.getAge(),
                request.getBodyFatRate(),
                request.getGoal(),
                nutritionResult.getTargetCalories(),
                nutritionResult.getProteinGram(),
                nutritionResult.getCarbsGram(),
                nutritionResult.getFatGram(),
                feedbackSection(feedback)
        );
    }

    static String alternatives(FoodAlternativeContext context) {
        return """
            너는 전문 식단 추천 API다.

            사용자의 하루 식단 중 한 끼에서 음식 하나를 바꾸려고 한다.
            바꿀 음식을 대신할 음식 %d개를 제안해라.

            반드시 아래 JSON 구조만 반환해라.
            설명 문장, 마크다운, 코드블록은 절대 반환하지 마라.

        조건:
        - 각 대안은 반드시 하나의 음식만 표현해라. 여러 음식을 합치지 마라.
        - 각 대안의 칼로리는 바꿀 음식의 ±10퍼센트 이내로 맞춰라.
        - 단백질·탄수화물·지방도 바꿀 음식과 최대한 비슷하게 맞춰라.
        - 같은 끼니의 나머지 음식과 어울리는, 한국에서 구하기 쉬운 음식으로 골라라.
        - 바꿀 음식, 끼니에 이미 있는 음식, 아래 '제안하지 말 음식'과 같은 음식은 제안하지 마라.
        - 대안끼리도 서로 다른 음식이어야 한다.
        - amount는 그 대안 음식 기준의 분량으로 써라.
        - shoppingKeyword는 쿠팡 같은 쇼핑몰에서 검색하기 좋은 짧은 키워드로 작성해라.
        %s
        goal=%s
        끼니=%s

        끼니의 나머지 음식:
        %s

        바꿀 음식:
        %s

        제안하지 말 음식:
        %s
        """.formatted(
                context.count(),
                preferenceRules(context.preferences()),
                context.goal(),
                context.mealType(),
                describeFoods(context.otherFoods()),
                describe(context.target()),
                context.avoidNames().isEmpty() ? "- 없음" : context.avoidNames().stream()
                        .map(name -> "- " + name).collect(Collectors.joining("\n"))
        );
    }

    static String mealRegeneration(MealRegenerationContext context) {
        MealRegenerationContext.MealTarget target = context.target();
        return """
            너는 전문 식단 추천 API다.

            사용자의 하루 식단 중 한 끼를 새로운 음식 조합으로 다시 구성하려고 한다.

            반드시 아래 JSON 구조만 반환해라.
            설명 문장, 마크다운, 코드블록은 절대 반환하지 마라.

        조건:
        - mealType은 반드시 %s 로 해라.
        - foods 배열의 각 원소는 반드시 하나의 음식만 표현해야 한다. 여러 음식을 합치지 마라.
        - 각 음식은 name, amount, calories, protein, carbohydrate, fat, shoppingKeyword를 포함해라.
        - 끼니 총 칼로리는 %d kcal의 ±10퍼센트 이내로 맞춰라.
        - 끼니 총 단백질 %.0fg, 탄수화물 %.0fg, 지방 %.0fg에 ±20퍼센트 이내로 맞춰라.
        - 지금 끼니의 음식은 되도록 다시 쓰지 말고 새로운 조합으로 구성해라.
        - 다른 끼니에 이미 있는 음식과 겹치지 않게 해라.
        - shoppingKeyword는 쿠팡 같은 쇼핑몰에서 검색하기 좋은 짧은 키워드로 작성해라.
        - 한국에서 일반적으로 구하기 쉬운 식재료 위주로 구성해라.
        %s%s
        goal=%s

        지금 끼니의 음식:
        %s

        다른 끼니의 음식:
        %s
        %s""".formatted(
                context.mealType(),
                target.calories(),
                target.protein(),
                target.carbohydrate(),
                target.fat(),
                preferenceRules(context.preferences()),
                hintSection(context.hints()),
                context.goal(),
                describeFoods(context.currentFoods()),
                describeFoods(context.otherMealFoods()),
                feedbackSection(context.feedback())
        );
    }

    private static String preferenceRules(FoodPreferences preferences) {
        if (preferences == null) {
            return "";
        }
        StringBuilder rules = new StringBuilder();
        if (!preferences.allergies().isEmpty()) {
            rules.append("- 사용자는 다음 분류에 알레르기가 있다. 이 분류의 식재료는 가공품·양념·소스·육수·가루를 포함해 어떤 형태로도 넣지 마라. 의심되면 빼라: ")
                    .append(labels(preferences.allergies()))
                    .append("\n");
        }
        if (!preferences.excluded().isEmpty()) {
            rules.append("- 다음 분류의 식재료는 가공품·양념·소스를 포함해 절대 사용하지 마라: ")
                    .append(labels(preferences.excluded()))
                    .append("\n");
        }
        if (!preferences.preferred().isEmpty()) {
            rules.append("- 가능하면 다음 분류의 식재료를 우선 활용해라: ")
                    .append(labels(preferences.preferred()))
                    .append("\n");
        }
        return rules.toString();
    }

    /** 강제 조건이 아니다. 영양 목표·알레르기·제외 규칙이 항상 먼저다. */
    private static String hintSection(PersonalizationHints hints) {
        if (hints == null || hints.isEmpty()) {
            return "";
        }
        StringBuilder section = new StringBuilder("- 아래는 사용자의 최근 기록이다. 위 조건을 모두 지킨 뒤에 참고만 해라.\n");
        appendHint(section, "최근 식단에 자주 나온 음식(같은 음식이 반복되지 않게 되도록 쓰지 마라)", hints.recentFoods());
        appendHint(section, "사용자가 식단에서 뺀 적 있는 음식(되도록 쓰지 마라)", hints.swappedOutFoods());
        appendHint(section, "사용자가 즐겨찾기한 음식(어울리면 한두 개 활용해도 좋다)", hints.favoriteFoods());
        appendHint(section, "사용자가 직접 바꿔 넣은 음식(취향에 가까운 음식이다)", hints.swappedInFoods());
        return section.toString();
    }

    private static void appendHint(StringBuilder section, String label, List<String> names) {
        if (!names.isEmpty()) {
            section.append("  - ").append(label).append(": ").append(String.join(", ", names)).append("\n");
        }
    }

    private static String feedbackSection(String feedback) {
        if (feedback == null || feedback.isBlank()) {
            return "";
        }
        return """

        이전에 생성한 결과에 다음 문제가 있었다. 이번에는 반드시 고쳐라:
        %s
        """.formatted(feedback);
    }

    private static String labels(Collection<FoodCategory> categories) {
        return categories.stream().map(FoodCategory::getLabel).collect(Collectors.joining(", "));
    }

    private static String describeFoods(List<FoodItem> foods) {
        if (foods == null || foods.isEmpty()) {
            return "- 없음";
        }
        return foods.stream().map(MealPlanPrompts::describe).collect(Collectors.joining("\n"));
    }

    private static String describe(FoodItem food) {
        return "- %s %s (%skcal, 단백질 %sg, 탄수화물 %sg, 지방 %sg)".formatted(
                food.getName(), food.getAmount(), food.getCalories(),
                food.getProtein(), food.getCarbohydrate(), food.getFat());
    }
}
