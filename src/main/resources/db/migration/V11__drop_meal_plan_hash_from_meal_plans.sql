ALTER TABLE meal_plans
DROP INDEX uk_meal_plans_user_hash;

ALTER TABLE meal_plans
DROP COLUMN meal_plan_hash;