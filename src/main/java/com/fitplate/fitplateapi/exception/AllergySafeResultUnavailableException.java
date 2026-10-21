// AI 결과에 알레르기 분류 음식이 계속 들어 있어 쓸 수 없을 때 던지는 예외입니다 (503).
package com.fitplate.fitplateapi.exception;

public class AllergySafeResultUnavailableException extends AiResultUnavailableException {
    public AllergySafeResultUnavailableException() {
        super("알레르기 음식을 빼고 식단을 만들지 못했어요. 잠시 후 다시 시도해주세요.");
    }
}
