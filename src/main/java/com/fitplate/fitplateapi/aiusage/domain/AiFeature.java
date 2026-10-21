// 하루 호출 상한을 두는 AI 기능과 그 상한입니다. 저장된 대안에서 꺼내는 건 세지 않습니다.
// 식단 생성은 하루 첫 번째만 광고 없이 만들 수 있습니다.
package com.fitplate.fitplateapi.aiusage.domain;

public enum AiFeature {
    GENERATION(3),
    ALTERNATIVES(20),
    MEAL_REGENERATION(5);

    private final int dailyLimit;

    AiFeature(int dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    public int dailyLimit() {
        return dailyLimit;
    }
}
