// AI 가 조건에 맞는 결과를 만들지 못했을 때 던지는 예외의 공통 부모입니다 (503).
package com.fitplate.fitplateapi.exception;

public class AiResultUnavailableException extends RuntimeException {
    public AiResultUnavailableException(String message) {
        super(message);
    }
}
