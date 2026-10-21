-- 식단을 특정 날짜에 지정(plan_date)하고 끼니별 섭취 시각(eaten_at)을 기록할 컬럼을 추가합니다.
-- 기존 식단은 날짜 없음(NULL)으로 두어 기록이 없는 날을 섭취 0 으로 만들지 않습니다.
ALTER TABLE meal_plans
    ADD COLUMN plan_date DATE NULL COMMENT '이 식단을 따르는 날짜(사용자당 하루 1개, NULL 이면 날짜 미지정)',
    ADD CONSTRAINT uk_meal_plans_user_plan_date UNIQUE (user_id, plan_date);

ALTER TABLE meal_plan_meals
    ADD COLUMN eaten_at DATETIME NULL COMMENT '끼니를 먹었다고 체크한 시각(NULL 이면 미섭취)';
