-- 알레르기 분류(제외보다 엄격하게 다룸)와 음식 교체 이력(개인화 추천에 활용)을 저장합니다.
ALTER TABLE user_profiles
    ADD COLUMN allergy_food_categories VARCHAR(255) NULL COMMENT '알레르기 음식 분류(쉼표 구분). 생성 결과에 남으면 그 결과를 쓰지 않음';

CREATE TABLE food_replacements (
    replacement_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '교체 이력 번호',
    user_id BIGINT NOT NULL COMMENT '바꾼 사용자 번호',
    from_name VARCHAR(255) NOT NULL COMMENT '뺀 음식 이름',
    to_name VARCHAR(255) NOT NULL COMMENT '넣은 음식 이름',
    created_at DATETIME NOT NULL COMMENT '바꾼 일시',
    PRIMARY KEY (replacement_id),
    KEY idx_food_replacements_user_created (user_id, created_at),
    CONSTRAINT fk_food_replacements_user FOREIGN KEY (user_id)
        REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
