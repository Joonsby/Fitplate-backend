-- 사용자가 직접 붙인 식단 이름입니다. NULL 이면 식단 음식으로 자동 이름을 만들어 보여줍니다.
ALTER TABLE meal_plans
    ADD COLUMN title VARCHAR(30) NULL COMMENT '사용자가 지정한 식단 이름. NULL 이면 자동 이름';
