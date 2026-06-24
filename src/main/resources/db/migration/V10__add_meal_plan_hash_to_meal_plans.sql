ALTER TABLE meal_plans
    ADD COLUMN meal_plan_hash VARCHAR(64) NULL;

UPDATE meal_plans
SET meal_plan_hash = SHA2(ai_response_json, 256)
WHERE meal_plan_hash IS NULL OR meal_plan_hash = '';

DELETE mp1
FROM meal_plans mp1
JOIN meal_plans mp2
  ON mp1.user_id = mp2.user_id
 AND mp1.meal_plan_hash = mp2.meal_plan_hash
 AND mp1.meal_plan_id > mp2.meal_plan_id;

ALTER TABLE meal_plans
    MODIFY COLUMN meal_plan_hash VARCHAR(64) NOT NULL;

ALTER TABLE meal_plans
    ADD CONSTRAINT uk_meal_plans_user_hash
        UNIQUE (user_id, meal_plan_hash);