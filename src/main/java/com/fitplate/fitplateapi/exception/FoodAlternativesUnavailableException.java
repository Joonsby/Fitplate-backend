// AI 가 조건에 맞는 대안 음식을 찾지 못했을 때 던지는 예외입니다 (503).
package com.fitplate.fitplateapi.exception;

public class FoodAlternativesUnavailableException extends AiResultUnavailableException {
    public FoodAlternativesUnavailableException() {
        this("조건에 맞는 다른 음식을 찾지 못했어요. 잠시 후 다시 시도해주세요.");
    }

    public FoodAlternativesUnavailableException(String message) {
        super(message);
    }
}
