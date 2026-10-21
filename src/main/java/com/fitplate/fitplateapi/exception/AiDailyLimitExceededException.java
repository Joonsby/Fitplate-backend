// 오늘 AI 기능을 부를 수 있는 횟수를 다 썼을 때 던지는 예외입니다 (429).
package com.fitplate.fitplateapi.exception;

public class AiDailyLimitExceededException extends RuntimeException {

    public static final String CODE = "AI_DAILY_LIMIT";

    public AiDailyLimitExceededException(String message) {
        super(message);
    }
}
