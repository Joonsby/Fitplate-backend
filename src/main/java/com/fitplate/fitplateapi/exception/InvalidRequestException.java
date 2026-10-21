// 요청 값이 규칙에 맞지 않을 때 던지는 예외입니다 (400).
package com.fitplate.fitplateapi.exception;

public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException(String message) {
        super(message);
    }
}
