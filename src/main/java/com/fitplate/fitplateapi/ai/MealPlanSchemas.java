// AI 응답 JSON 스키마를 만듭니다. strict 는 OpenAI(additionalProperties 필수)용입니다.
package com.fitplate.fitplateapi.ai;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class MealPlanSchemas {

    private MealPlanSchemas() {
    }

    /** { meals: [ { mealType, title, foods: [food] } ] } */
    static Map<String, Object> mealPlan(boolean strict) {
        return object(strict,
                Map.of("meals", Map.of("type", "array", "items", meal(strict))),
                List.of("meals"));
    }

    /** { mealType, title, foods: [food] } */
    static Map<String, Object> meal(boolean strict) {
        return object(strict,
                Map.of(
                        "mealType", Map.of(
                                "type", "string",
                                "enum", List.of("breakfast", "lunch", "dinner")
                        ),
                        "title", Map.of("type", "string"),
                        "foods", Map.of("type", "array", "items", food(strict))
                ),
                List.of("mealType", "title", "foods"));
    }

    /** { alternatives: [food] } */
    static Map<String, Object> alternatives(boolean strict) {
        return object(strict,
                Map.of("alternatives", Map.of("type", "array", "items", food(strict))),
                List.of("alternatives"));
    }

    private static Map<String, Object> food(boolean strict) {
        return object(strict,
                Map.of(
                        "name", Map.of("type", "string"),
                        "amount", Map.of("type", "string"),
                        "calories", Map.of("type", "integer"),
                        "protein", Map.of("type", "number"),
                        "carbohydrate", Map.of("type", "number"),
                        "fat", Map.of("type", "number"),
                        "shoppingKeyword", Map.of("type", "string")
                ),
                List.of("name", "amount", "calories", "protein", "carbohydrate", "fat", "shoppingKeyword"));
    }

    private static Map<String, Object> object(boolean strict, Map<String, Object> properties, List<String> required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        if (strict) {
            schema.put("additionalProperties", false);
        }
        schema.put("properties", properties);
        schema.put("required", required);
        return schema;
    }
}
