-- 사용자 프로필에 선호·제외 음식 분류를 저장할 컬럼을 추가합니다.
ALTER TABLE user_profiles
    ADD COLUMN preferred_food_categories VARCHAR(255) NULL COMMENT '선호 음식 분류(쉼표 구분, 예: CHICKEN,SEAFOOD)',
    ADD COLUMN excluded_food_categories VARCHAR(255) NULL COMMENT '제외 음식 분류(쉼표 구분, 예: PORK,NUTS)';
