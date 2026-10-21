-- 날짜별 체중 기록 테이블과 프로필의 목표 체중 컬럼을 추가합니다.
-- 같은 사용자의 같은 날짜는 기록 하나만 둡니다 (다시 기록하면 덮어씀).
CREATE TABLE weight_records (
    weight_record_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '체중 기록 번호',
    user_id BIGINT NOT NULL COMMENT '기록한 사용자 번호',
    record_date DATE NOT NULL COMMENT '체중을 잰 날짜',
    weight DECIMAL(5,2) NOT NULL COMMENT '체중(kg)',
    created_at DATETIME NOT NULL COMMENT '처음 기록한 일시',
    updated_at DATETIME NOT NULL COMMENT '마지막으로 고친 일시',
    PRIMARY KEY (weight_record_id),
    CONSTRAINT uk_weight_records_user_date UNIQUE (user_id, record_date),
    CONSTRAINT fk_weight_records_user FOREIGN KEY (user_id)
        REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT chk_weight_records_weight CHECK (weight BETWEEN 20 AND 300)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE user_profiles
    ADD COLUMN target_weight DECIMAL(5,2) NULL COMMENT '목표 체중(kg). NULL 이면 설정 안 함';
