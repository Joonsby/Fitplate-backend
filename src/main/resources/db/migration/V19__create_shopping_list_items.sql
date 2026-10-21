-- 식단별 장보기 리스트 항목을 저장합니다. 식단 음식에서 만든 항목(GENERATED)과 사용자가 직접 넣은 항목(MANUAL)을 함께 둡니다.
-- AI 결과에는 재료별 정확한 수량이 없어 quantity_text 는 식단에 적힌 분량을 합치거나 이어 붙인 표시용 문구입니다.
CREATE TABLE shopping_list_items (
    item_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '장보기 항목 번호',
    meal_plan_id BIGINT NOT NULL COMMENT '항목이 속한 식단 번호',
    name VARCHAR(100) NOT NULL COMMENT '살 것 (쇼핑 검색 키워드 기준)',
    quantity_text VARCHAR(100) NULL COMMENT '표시용 분량 (예: 300g, 150g + 1공기). 모르면 NULL',
    note VARCHAR(255) NULL COMMENT '어떤 음식에 쓰는지',
    source VARCHAR(10) NOT NULL COMMENT '항목 출처(GENERATED: 식단에서 생성, MANUAL: 직접 추가)',
    checked BIT(1) NOT NULL DEFAULT b'0' COMMENT '장바구니에 담았는지',
    sort_order INT NOT NULL COMMENT '표시 순서',
    created_at DATETIME NOT NULL COMMENT '추가 일시',
    updated_at DATETIME NOT NULL COMMENT '수정 일시',
    PRIMARY KEY (item_id),
    KEY idx_shopping_list_items_plan (meal_plan_id, sort_order),
    CONSTRAINT fk_shopping_list_items_plan FOREIGN KEY (meal_plan_id)
        REFERENCES meal_plans (meal_plan_id) ON DELETE CASCADE,
    CONSTRAINT chk_shopping_list_items_source CHECK (source IN ('GENERATED', 'MANUAL'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
