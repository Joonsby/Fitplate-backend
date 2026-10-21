// AI 가 조건에 맞는 끼니를 다시 만들지 못했을 때 던지는 예외입니다 (503).
package com.fitplate.fitplateapi.exception;

public class MealRegenerationUnavailableException extends AiResultUnavailableException {
    public MealRegenerationUnavailableException() {
        super("조건에 맞는 끼니를 다시 만들지 못했어요. 잠시 후 다시 시도해주세요.");
    }
}
