-- 모든 테이블 컬럼에 한글 설명을 추가하고 기존 자료형과 기본값을 유지합니다.
ALTER TABLE users
    MODIFY COLUMN user_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '사용자 고유 번호',
    MODIFY COLUMN toss_user_key VARCHAR(100) NOT NULL COMMENT '토스에서 발급한 사용자 식별 키',
    MODIFY COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '사용자 등록 일시';

ALTER TABLE user_profiles
    MODIFY COLUMN user_id BIGINT NOT NULL COMMENT '프로필 소유 사용자 번호',
    MODIFY COLUMN height INT NOT NULL COMMENT '현재 키(cm)',
    MODIFY COLUMN weight INT NOT NULL COMMENT '현재 체중(kg)',
    MODIFY COLUMN age INT NOT NULL COMMENT '현재 나이(세)',
    MODIFY COLUMN gender VARCHAR(10) NOT NULL COMMENT '성별(MALE: 남성, FEMALE: 여성)',
    MODIFY COLUMN bmi DECIMAL(5,2) DEFAULT NULL COMMENT '현재 체질량지수',
    MODIFY COLUMN body_fat_rate DECIMAL(5,2) DEFAULT NULL COMMENT '현재 체지방률(%)',
    MODIFY COLUMN created_at DATETIME NOT NULL COMMENT '프로필 생성 일시',
    MODIFY COLUMN updated_at DATETIME NOT NULL COMMENT '프로필 최종 수정 일시';

ALTER TABLE meal_plans
    MODIFY COLUMN meal_plan_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '식단 계획 고유 번호',
    MODIFY COLUMN user_id BIGINT NOT NULL COMMENT '식단 소유 사용자 번호',
    MODIFY COLUMN goal VARCHAR(20) NOT NULL COMMENT '생성 당시 목표(MAINTAIN: 유지, WEIGHT_LOSS: 감량, WEIGHT_GAIN: 증량)',
    MODIFY COLUMN height INT NOT NULL COMMENT '식단 생성 당시 키(cm)',
    MODIFY COLUMN weight INT NOT NULL COMMENT '식단 생성 당시 체중(kg)',
    MODIFY COLUMN age INT NOT NULL COMMENT '식단 생성 당시 나이(세)',
    MODIFY COLUMN gender VARCHAR(10) NOT NULL COMMENT '식단 생성 당시 성별(MALE: 남성, FEMALE: 여성)',
    MODIFY COLUMN target_calories INT NOT NULL COMMENT '하루 목표 섭취 열량(kcal)',
    MODIFY COLUMN bmr INT NOT NULL COMMENT '생성 당시 기초대사량(kcal/일)',
    MODIFY COLUMN tdee INT NOT NULL COMMENT '생성 당시 하루 총 에너지 소비량(kcal/일)',
    MODIFY COLUMN protein_gram INT NOT NULL COMMENT '하루 목표 단백질 섭취량(g)',
    MODIFY COLUMN carbs_gram INT NOT NULL COMMENT '하루 목표 탄수화물 섭취량(g)',
    MODIFY COLUMN fat_gram INT NOT NULL COMMENT '하루 목표 지방 섭취량(g)',
    MODIFY COLUMN ai_response_json JSON NOT NULL COMMENT '생성된 식단 응답을 보존한 JSON',
    MODIFY COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '식단 생성 일시',
    MODIFY COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '식단 최종 수정 일시',
    MODIFY COLUMN bmi DECIMAL(5,2) NOT NULL COMMENT '식단 생성 당시 체질량지수',
    MODIFY COLUMN body_fat_rate DECIMAL(5,2) DEFAULT NULL COMMENT '식단 생성 당시 체지방률(%)';

ALTER TABLE meal_plan_meals
    MODIFY COLUMN meal_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '끼니 고유 번호',
    MODIFY COLUMN meal_plan_id BIGINT NOT NULL COMMENT '소속 식단 계획 번호',
    MODIFY COLUMN meal_type VARCHAR(20) NOT NULL COMMENT '끼니 구분(breakfast: 아침, lunch: 점심, dinner: 저녁)',
    MODIFY COLUMN title VARCHAR(255) NOT NULL COMMENT '끼니 제목',
    MODIFY COLUMN sort_order INT NOT NULL COMMENT '식단 내 끼니 표시 순서(0부터 시작)';

ALTER TABLE meal_plan_foods
    MODIFY COLUMN food_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '식단 음식 고유 번호',
    MODIFY COLUMN meal_id BIGINT NOT NULL COMMENT '소속 끼니 번호',
    MODIFY COLUMN name VARCHAR(255) NOT NULL COMMENT '생성 당시 음식 이름',
    MODIFY COLUMN amount VARCHAR(100) NOT NULL COMMENT '음식 섭취 분량과 단위',
    MODIFY COLUMN calories INT NOT NULL COMMENT '해당 분량의 열량(kcal)',
    MODIFY COLUMN protein DECIMAL(8,2) NOT NULL COMMENT '해당 분량의 단백질(g)',
    MODIFY COLUMN carbohydrate DECIMAL(8,2) NOT NULL COMMENT '해당 분량의 탄수화물(g)',
    MODIFY COLUMN fat DECIMAL(8,2) NOT NULL COMMENT '해당 분량의 지방(g)',
    MODIFY COLUMN shopping_keyword VARCHAR(255) NOT NULL COMMENT '음식 구매 검색어',
    MODIFY COLUMN sort_order INT NOT NULL COMMENT '끼니 내 음식 표시 순서(0부터 시작)';

ALTER TABLE favorite_foods
    MODIFY COLUMN favorite_food_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '즐겨찾기 음식 고유 번호',
    MODIFY COLUMN user_id BIGINT NOT NULL COMMENT '즐겨찾기 소유 사용자 번호',
    MODIFY COLUMN food_name VARCHAR(100) NOT NULL COMMENT '즐겨찾기에 보관한 음식 이름',
    MODIFY COLUMN amount VARCHAR(50) DEFAULT NULL COMMENT '보관한 음식 분량과 단위',
    MODIFY COLUMN calories INT DEFAULT NULL COMMENT '보관한 분량의 열량(kcal)',
    MODIFY COLUMN carbohydrate DECIMAL(8,2) DEFAULT NULL COMMENT '보관한 분량의 탄수화물(g)',
    MODIFY COLUMN protein DECIMAL(8,2) DEFAULT NULL COMMENT '보관한 분량의 단백질(g)',
    MODIFY COLUMN fat DECIMAL(8,2) DEFAULT NULL COMMENT '보관한 분량의 지방(g)',
    MODIFY COLUMN shopping_category VARCHAR(50) DEFAULT NULL COMMENT '구매용 음식 분류',
    MODIFY COLUMN shopping_keyword VARCHAR(100) DEFAULT NULL COMMENT '음식 구매 검색어',
    MODIFY COLUMN source_food_id VARCHAR(100) DEFAULT NULL COMMENT '즐겨찾기 등록 당시 원본 음식 식별 문자열',
    MODIFY COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '즐겨찾기 등록 일시',
    MODIFY COLUMN updated_at DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '즐겨찾기 최종 수정 일시';

ALTER TABLE flyway_schema_history
    MODIFY COLUMN installed_rank INT NOT NULL COMMENT '마이그레이션 적용 순번',
    MODIFY COLUMN version VARCHAR(50) DEFAULT NULL COMMENT '마이그레이션 버전',
    MODIFY COLUMN description VARCHAR(200) NOT NULL COMMENT '마이그레이션 설명',
    MODIFY COLUMN type VARCHAR(20) NOT NULL COMMENT '마이그레이션 실행 유형',
    MODIFY COLUMN script VARCHAR(1000) NOT NULL COMMENT '실행한 마이그레이션 파일 이름',
    MODIFY COLUMN checksum INT DEFAULT NULL COMMENT '마이그레이션 파일 검증값',
    MODIFY COLUMN installed_by VARCHAR(100) NOT NULL COMMENT '마이그레이션 실행 계정',
    MODIFY COLUMN installed_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '마이그레이션 적용 일시',
    MODIFY COLUMN execution_time INT NOT NULL COMMENT '마이그레이션 실행 시간(밀리초)',
    MODIFY COLUMN success TINYINT(1) NOT NULL COMMENT '마이그레이션 성공 여부';
