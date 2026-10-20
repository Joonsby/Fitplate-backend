-- 기존 식단 JSON의 끼니와 음식을 원본 순서대로 관계형 테이블에 이전합니다.
START TRANSACTION;

INSERT INTO meal_plan_meals (meal_plan_id, meal_type, title, sort_order)
SELECT p.meal_plan_id, j.meal_type, j.title, j.meal_position - 1
FROM meal_plans p
JOIN JSON_TABLE(
    p.ai_response_json,
    '$.meals[*]' COLUMNS (
        meal_position FOR ORDINALITY,
        meal_type VARCHAR(20) PATH '$.mealType' ERROR ON EMPTY ERROR ON ERROR,
        title VARCHAR(255) PATH '$.title' ERROR ON EMPTY ERROR ON ERROR
    )
) AS j ON TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM meal_plan_meals existing
    WHERE existing.meal_plan_id = p.meal_plan_id
      AND existing.meal_type = j.meal_type
)
ORDER BY p.meal_plan_id, j.meal_position;

INSERT INTO meal_plan_foods (
    meal_id, name, amount, calories, protein, carbohydrate, fat,
    shopping_keyword, sort_order
)
SELECT m.meal_id, j.food_name, j.amount, j.calories, j.protein,
       j.carbohydrate, j.fat, j.shopping_keyword, j.food_position - 1
FROM meal_plans p
JOIN JSON_TABLE(
    p.ai_response_json,
    '$.meals[*]' COLUMNS (
        meal_position FOR ORDINALITY,
        meal_type VARCHAR(20) PATH '$.mealType' ERROR ON EMPTY ERROR ON ERROR,
        NESTED PATH '$.foods[*]' COLUMNS (
            food_position FOR ORDINALITY,
            food_name VARCHAR(255) PATH '$.name' ERROR ON EMPTY ERROR ON ERROR,
            amount VARCHAR(100) PATH '$.amount' ERROR ON EMPTY ERROR ON ERROR,
            calories INT PATH '$.calories' ERROR ON EMPTY ERROR ON ERROR,
            protein DECIMAL(8,2) PATH '$.protein' ERROR ON EMPTY ERROR ON ERROR,
            carbohydrate DECIMAL(8,2) PATH '$.carbohydrate' ERROR ON EMPTY ERROR ON ERROR,
            fat DECIMAL(8,2) PATH '$.fat' ERROR ON EMPTY ERROR ON ERROR,
            shopping_keyword VARCHAR(255) PATH '$.shoppingKeyword' ERROR ON EMPTY ERROR ON ERROR
        )
    )
) AS j ON TRUE
JOIN meal_plan_meals m
    ON m.meal_plan_id = p.meal_plan_id
   AND m.meal_type = j.meal_type
   AND m.sort_order = j.meal_position - 1
WHERE NOT EXISTS (
    SELECT 1 FROM meal_plan_foods existing
    WHERE existing.meal_id = m.meal_id
      AND existing.sort_order = j.food_position - 1
)
ORDER BY p.meal_plan_id, j.meal_position, j.food_position;

COMMIT;
