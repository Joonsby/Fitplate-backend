-- 식단의 끼니와 음식을 저장할 테이블 및 조회 인덱스를 추가합니다.
CREATE TABLE meal_plan_meals (
    meal_id BIGINT NOT NULL AUTO_INCREMENT,
    meal_plan_id BIGINT NOT NULL,
    meal_type VARCHAR(20) NOT NULL,
    title VARCHAR(255) NOT NULL,
    sort_order INT NOT NULL,
    PRIMARY KEY (meal_id),
    CONSTRAINT uk_meal_plan_meals_plan_type UNIQUE (meal_plan_id, meal_type),
    CONSTRAINT uk_meal_plan_meals_plan_order UNIQUE (meal_plan_id, sort_order),
    CONSTRAINT fk_meal_plan_meals_plan FOREIGN KEY (meal_plan_id)
        REFERENCES meal_plans (meal_plan_id) ON DELETE CASCADE,
    CONSTRAINT chk_meal_plan_meals_type
        CHECK (meal_type IN ('breakfast', 'lunch', 'dinner')),
    CONSTRAINT chk_meal_plan_meals_order CHECK (sort_order >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE meal_plan_foods (
    food_id BIGINT NOT NULL AUTO_INCREMENT,
    meal_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    amount VARCHAR(100) NOT NULL,
    calories INT NOT NULL,
    protein DECIMAL(8,2) NOT NULL,
    carbohydrate DECIMAL(8,2) NOT NULL,
    fat DECIMAL(8,2) NOT NULL,
    shopping_keyword VARCHAR(255) NOT NULL,
    sort_order INT NOT NULL,
    PRIMARY KEY (food_id),
    CONSTRAINT uk_meal_plan_foods_meal_order UNIQUE (meal_id, sort_order),
    CONSTRAINT fk_meal_plan_foods_meal FOREIGN KEY (meal_id)
        REFERENCES meal_plan_meals (meal_id) ON DELETE CASCADE,
    CONSTRAINT chk_meal_plan_foods_calories CHECK (calories >= 0),
    CONSTRAINT chk_meal_plan_foods_protein CHECK (protein >= 0),
    CONSTRAINT chk_meal_plan_foods_carbohydrate CHECK (carbohydrate >= 0),
    CONSTRAINT chk_meal_plan_foods_fat CHECK (fat >= 0),
    CONSTRAINT chk_meal_plan_foods_order CHECK (sort_order >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_meal_plans_user_created_id
    ON meal_plans (user_id, created_at, meal_plan_id);
