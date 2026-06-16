ALTER TABLE meal_plans
    RENAME COLUMN height_cm TO height;

ALTER TABLE meal_plans
    RENAME COLUMN weight_kg TO weight;

ALTER TABLE user_profiles
    RENAME COLUMN height_cm TO height;

ALTER TABLE user_profiles
    RENAME COLUMN weight_kg TO weight;