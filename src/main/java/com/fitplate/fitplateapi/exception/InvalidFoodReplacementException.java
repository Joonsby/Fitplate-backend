// 바꿀 음식이 영양 균형 조건을 벗어날 때 던지는 예외입니다 (400).
package com.fitplate.fitplateapi.exception;

public class InvalidFoodReplacementException extends RuntimeException {
    public InvalidFoodReplacementException(String message) {
        super(message);
    }
}
