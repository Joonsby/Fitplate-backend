-- AI 가 만든 음식 대안을 모든 사용자가 다시 쓰도록 저장하고, 사용자별 하루 AI 호출 횟수를 셉니다.
CREATE TABLE food_alternative_pool (
    entry_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '대안 번호',
    source_key VARCHAR(255) NOT NULL COMMENT '바꿀 음식 이름(공백 제거)',
    candidate_key VARCHAR(255) NOT NULL COMMENT '대안 음식 이름(공백 제거). 같은 원래 음식 안에서 중복 방지',
    name VARCHAR(255) NOT NULL COMMENT '대안 음식 이름',
    amount VARCHAR(100) NOT NULL COMMENT '대안 분량',
    calories INT NOT NULL COMMENT '대안 칼로리(kcal)',
    protein DECIMAL(8, 2) NOT NULL COMMENT '단백질(g)',
    carbohydrate DECIMAL(8, 2) NOT NULL COMMENT '탄수화물(g)',
    fat DECIMAL(8, 2) NOT NULL COMMENT '지방(g)',
    shopping_keyword VARCHAR(255) NOT NULL COMMENT '쇼핑 검색어',
    created_at DATETIME NOT NULL COMMENT '저장 일시',
    PRIMARY KEY (entry_id),
    UNIQUE KEY uk_food_alternative_pool_source_candidate (source_key, candidate_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE ai_daily_usage (
    usage_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '사용량 번호',
    user_id BIGINT NOT NULL COMMENT '사용자 번호',
    usage_date DATE NOT NULL COMMENT '사용 날짜(서울 기준)',
    feature VARCHAR(30) NOT NULL COMMENT 'ALTERNATIVES: 음식 대안, MEAL_REGENERATION: 끼니 재생성',
    used_count INT NOT NULL COMMENT '그날 AI 를 부른 횟수',
    PRIMARY KEY (usage_id),
    UNIQUE KEY uk_ai_daily_usage_user_date_feature (user_id, usage_date, feature),
    CONSTRAINT fk_ai_daily_usage_user FOREIGN KEY (user_id)
        REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
